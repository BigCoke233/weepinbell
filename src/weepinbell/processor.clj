(ns weepinbell.processor
  "Process in-coming webmentions.
   If no such webmention exists and request is verified, create.
   If exists and verification fails, delete it.
   If exists and verification passes, update it."
  (:require [clojure.core.async :refer [go-loop <!]]
            [weepinbell.wrapper :as wrapper]
            [weepinbell.verifier :refer [verify-mention]]
            [weepinbell.utils.digest :refer [digest-short]]
            [taoensso.timbre :as timbre]))

(defn process-new-webmention
  "Store new webmention if verifed. Do nothing otherwise."
  [id source target callbacks storage config]
  (when (verify-mention source target)
    (timbre/info "new webmention" id "received.")
    (let [data (wrapper/wrap source target (:base-url config))]
      ((:append storage) data)))
  ((:on-create callbacks)))

(defn process-existing-webmention
  "Update existing Webmention when verified, delete it otherwise."
  [id source target callbacks storage config]
  (timbre/info "webmention" id "already exists.")
  (if (verify-mention source target)
    (do
      ((:update storage) source target
                         (wrapper/wrap source target (:base-url config)))
      ((:on-update callbacks))
      (timbre/info "webmention" id "updated."))
    (do
      ((:delete storage) source target)
      ((:on-delete callbacks))
      (timbre/warn "webmention" id "no longer valid, deleted."))))

(defn process-webmention
  "Process webmention one webmention request.
   Validate source's Content-Type, inclusion of target URL.
   If validated, create webmention when no identical is found,
   update or delete if webmention already exists.
   
   Callbacks and storage are maps of functions, to avoid
   depending on technical details directly."
  [wm callbacks storage config]
  (let [{:keys [s t]} wm
        id (digest-short (str s t))]
    ;; check if such webmention exists
    (if ((:get storage) s t)
      ;; if so, update it or delete it
      (process-existing-webmention id s t callbacks storage config)
      ;; if not, write to data
      (process-new-webmention id s t callbacks storage config))
    ((:on-receive callbacks))))

(defn start
  "Start a go-loop that recursively calls to process-webmention."
  [wmc callbacks storage config]
  (go-loop [wm (<! wmc)]
    (timbre/info "webmention found in queue, processing.")
    (process-webmention wm callbacks storage config)
    (recur (<! wmc))))

