(ns weepinbell.processor
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.string :refer [includes? lower-case]]
            [clj-http.client :as client]
            [weepinbell.storage :as storage]
            [weepinbell.utils :refer [validate-content-types
                                      javamap->map
                                      file-exists?
                                      download-file]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.webhook :as webhook]
            [taoensso.timbre :as timbre]
            [digest :as digest])
  (:import [com.kylewm.mf2j Mf2Parser]
           [java.net URI]))

(defn verify-mention
  "Get source and follow redirects to check if target link is present."
  [source target]
  (try
    (timbre/info (str "Verifying mention from: " source))
    ;; get first 1MB of source URL and verify
    (let [{:keys [status body]} (client/get source {:throw false
                                                    :headers {"Range" "bytes=0-1048575"}})]
      (if (and (>= status 200) (< status 300))
        ;; TODO needs to be more robust
        (if (includes? (str body) target)
          true
          (do (timbre/error "Target link not found in source body.") false))
        (do (timbre/error "Source returned status:" status) false)))
    (catch Exception e
      (timbre/error "Error verifying mention:" (.getMessage e))
      false)))

(defn parse-microformats
  "Parse microformats from a  :as digestURL."
  [url]
  (let [max-retries 3]
    (loop [attempt 1]
      (let [result (try
                     (let [parser (doto (Mf2Parser.)
                                    (.setIncludeAlternates true)
                                    (.setIncludeRelUrls true))]
                       {:ok true :data (.parse parser (URI. url))})
                     (catch Exception e
                       {:ok false :exception e}))]
        (if (:ok result)
          (:data result)
          (let [e (:exception result)
                msg (.getMessage e)]
            (if (and (<= attempt max-retries)
                     (or (instance? java.net.SocketTimeoutException e)
                         (clojure.string/includes? (lower-case (str msg)) "timeout")))
              (let [wait-time (* attempt 2000)]
                (timbre/warn (format "MF2 Parsing timeout for %s (attempt %d/%d). Retrying in %dms..."
                                 url attempt max-retries wait-time))
                (Thread/sleep wait-time)
                (recur (inc attempt)))
              (do
                (timbre/warn (format "MF2 Parsing failed for %s after %d attempts. Error: %s"
                                 url attempt (or msg "Unknown error")))
                nil))))))))


(defn wrap-webmention
  "Parse microformats and collect essential info. Wrap source, target and collected data into a Clojure map and return it."
  [source target]
  (let [mf (-> source
               parse-microformats
               javamap->map
               :items first
               javamap->map)
        wm {:source source
            :target target
            :id (digest/md5 (str source target))
            :modified (System/currentTimeMillis)}
        raw-photo (or (-> mf
                          (get-in [:properties "author"])
                          first
                          (get-in ["properties" "photo"])
                          first)
                      (-> mf (get-in [:properties "photo"]) first))
          ;; download if found photo
        proxied-photo (if raw-photo
               (let [photo-url (str raw-photo)
                     file-ext (or (re-find #"\.(?:jpg|jpeg|png|gif|webp)" photo-url) ".jpg")
                     filename  (str (digest/md5 photo-url) file-ext)
                     local-path (str "data/avatars/" filename)
                     public-path (str "/avatars/" filename)]
                 ;; download photo if not exists
                 (when-not (file-exists? local-path)
                   (timbre/info "Downloading microformats photo to proxy external image.")
                   (download-file photo-url local-path))
                 (str (get-config :base-url) public-path))
                        nil)]
      {:webmention wm
       :microformats mf
       :extra {:proxied-photo proxied-photo}}))

(defn process-new-webmention
  "Store new webmention if verifed. Do nothing otherwise."
  [source target]
  (when (verify-mention source target)
    (timbre/info "\n🙋‍♂️ Valid webmention received: \n"
      "from " source "\n"
      "to " target "\n\n")
    (let [data (wrap-webmention source target)]
      (storage/append-wm data))))

(defn process-existing-webmention
  "Update existing Webmention when verified, delete it otherwise."
  [source target]
  (timbre/info "Webmention exists.")
  (if (verify-mention source target)
    (do
      (storage/update-wm source target
                         (wrap-webmention source target))
      (timbre/info "Webmention updated."))
    (do
      (storage/delete-wm source target)
      (timbre/info "Webmention no longer valid, deleted."))))

(defn process-webmention
  [wmc]
  (go-loop []
    (when-let [webmention (<! wmc)]
      (timbre/info "Webmention found in queue.")
      (let [{:keys [s t]} webmention]
       ;; first check Content-Types of both urls
       ;; only proceed when it's HTML or plain text
        (if (validate-content-types s t)
         ;; second check if such webmention exists
         ;; if so, update it or delete it
         ;; if not, write to data
          (do
            (if (storage/get-wm s t)
              (process-existing-webmention s t)
              (process-new-webmention s t))
            ;; trigger webhook
            (webhook/trigger :on-receive))
          ;; if content-type check fails
          (timbre/error "Source or target is not HTML or plain text; or error occurred trying to get content type."))))
    (recur)))

