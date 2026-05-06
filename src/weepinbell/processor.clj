(ns weepinbell.processor
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.string :refer [includes?]]
            [clj-http.client :as client]
            [weepinbell.utils :refer [validate-content-types]]
            [weepinbell.wrapper :as wrapper]
            [taoensso.timbre :as timbre]))

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

(defn process-new-webmention
  "Store new webmention if verifed. Do nothing otherwise."
  [source target callbacks storage]
  (when (verify-mention source target)
    (timbre/info "\n🙋‍♂️ Valid webmention received: \n"
      "from " source "\n"
      "to " target "\n\n")
    (let [data (wrapper/wrap source target)]
      ((:append storage) data)))
  ((:on-create callbacks)))

(defn process-existing-webmention
  "Update existing Webmention when verified, delete it otherwise."
  [source target callbacks storage]
  (timbre/info "Webmention exists.")
  (if (verify-mention source target)
    (do
      ((:update storage) source target
                         (wrapper/wrap source target))
      ((:on-update callbacks))
      (timbre/info "Webmention updated."))
    (do
      ((:delete storage) source target)
      ((:on-delete callbacks))
      (timbre/info "Webmention no longer valid, deleted."))))

(defn process-webmention
  "Process webmention one webmention request.
   Validate source's Content-Type, inclusion of target URL.
   If validated, create webmention when no identical is found,
   update or delete if webmention already exists.
   
   Callbacks and storage are maps of functions, to avoid
   depending on technical details directly."
  [wm callbacks storage]
  (let [{:keys [s t]} wm]
   ;; first check Content-Types of both urls
   ;; only proceed when it's HTML or plain text
    (if (validate-content-types s t)
     ;; second check if such webmention exists
     ;; if so, update it or delete it
     ;; if not, write to data
      (do
        (if ((:get storage) s t)
          (process-existing-webmention s t callbacks storage)
          (process-new-webmention s t callbacks storage))
        ((:on-receive callbacks)))
      ;; if content-type check fails
      (timbre/error "Source or target is not HTML or plain text; or error occurred trying to get content type."))))

(defn start
  "Start a go-loop that recursively calls to process-webmention."
  [wmc callbacks storage]
  (go-loop [wm (<! wmc)]
           (timbre/info "Webmention found in queue.")
           (process-webmention wm callbacks storage)
           (recur (<! wmc))))

