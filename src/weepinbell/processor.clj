(ns weepinbell.processor
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.string :refer [includes?]]
            [clj-http.client :as client]
            [weepinbell.storage :as storage]
            [clojure.walk :as walk])
  (:import [com.kylewm.mf2j Mf2Parser]
           [java.net URI]))

(defn verify-mention
  "Get source and follow redirects to check if target link is present."
  [source target]
  ;; the client follows redirects automatically
  (let [{:keys [body]}  (client/get source)]
    (println "Verifying mention")
    ;; check if body contains source
    ;; TODO needs to be more robust
    (includes? body target)))

(defn parse-microformats
  "Parse microformats from a URL."
  [url]
  (let [parser (doto (Mf2Parser.)
                 (.setIncludeAlternates true)
                 (.setIncludeRelUrls true))]
    (.parse parser (URI. url))))

(defn javamap->map [map]
  (->> (seq map) (into {}) walk/keywordize-keys))

(defn wrap-webmention
  "Parse microformats and collect essential info. Wrap source, target and collected data into a Clojure map and return it."
  [source target]
  (let [mf (-> source
               parse-microformats
               javamap->map
               :items first
               javamap->map)
        wm {:source source :target target}]
    {:webmention wm :microformats mf}))

(defn process-new-webmention
  "If a webmention is verified, fetch microformats data and store it."
  [source target]
  ;; log
  (println "\n🙋‍♂️ Valid webmention received:")
  (println "from " source)
  (println "to " target "\n")
  ;; wrap and store data
  (let [data (wrap-webmention source target)]
    (storage/append-wm data)))

(defn process-existing-webmention
  "Update existing Webmention when verified, delete it otherwise."
  [verified? source target]
  (if verified?
    (storage/update-wm source target
                    (wrap-webmention source target))
    (storage/delete-wm source target)))

(defn process-webmention
  "Retrieve webmention from queue and process it."
  [wmc]
  (println "Processor started.")
  (go-loop []
    (when-let [wm (<! wmc)]
      (println "Webmention found in queue.")
      (let [{:keys [s t]} wm
            verified? (verify-mention s t)]
        ;; handle existing webmention and new webmention differently
        (if (storage/get-wm s t)
          (process-existing-webmention verified? s t)
          (when verified? (process-new-webmention s t))))
      (println "Idling"))
    (recur)))

