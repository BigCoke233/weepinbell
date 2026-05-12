(ns weepinbell.processor
  "Process in-coming webmentions.
   If no such webmention exists and request is verified, create.
   If exists and verification fails, delete it.
   If exists and verification passes, update it."
  (:require [clojure.core.async :refer [go-loop <!]]
            [weepinbell.wrapper :as wrapper]
            [weepinbell.verifier :refer [verify-mention]]
            [taoensso.timbre :as timbre]))

(defn process-new-webmention
  "Store new webmention if verifed. Do nothing otherwise."
  [source target callbacks storage config]
  (when (verify-mention source target)
    (timbre/info "\n🙋‍♂️ Valid webmention received: \n"
                 "from " source "\n"
                 "to " target "\n\n")
    (let [data (wrapper/wrap source target (:base-url config))]
      ((:append storage) data)))
  ((:on-create callbacks)))

(defn process-existing-webmention
  "Update existing Webmention when verified, delete it otherwise."
  [source target callbacks storage config]
  (timbre/info "Webmention exists.")
  (if (verify-mention source target)
    (do
      ((:update storage) source target
                         (wrapper/wrap source target (:base-url config)))
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
  [wm callbacks storage config]
  (let [{:keys [s t]} wm]
     ;; check if such webmention exists
     ;; if so, update it or delete it
     ;; if not, write to data
    (do
      (if ((:get storage) s t)
        (process-existing-webmention s t callbacks storage config)
        (process-new-webmention s t callbacks storage config))
      ((:on-receive callbacks)))))

(defn start
  "Start a go-loop that recursively calls to process-webmention."
  [wmc callbacks storage config]
  (go-loop [wm (<! wmc)]
    (timbre/info "Webmention found in queue.")
    (process-webmention wm callbacks storage config)
    (recur (<! wmc))))

