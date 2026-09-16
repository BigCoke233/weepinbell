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
      ((:append storage) data)
      ((:on-create callbacks) data))))

(defn process-existing-webmention
  "Update existing Webmention when verified, delete it otherwise."
  [wm-data callbacks storage config]
  (let [webmention (:webmention wm-data)
        id (:id webmention)
        source (:source webmention)
        target (:target webmention)]
    (timbre/info "webmention" id "already exists.")
    (if (verify-mention source target)
      (let [data (wrapper/wrap source target (:base-url config))]
        ((:update storage) source target data)
        ((:on-update callbacks) data)
        (timbre/info "webmention" id "updated."))
      (do
        ((:delete storage) source target)
        ((:on-delete callbacks) wm-data)
        (timbre/warn "webmention" id "no longer valid, deleted.")))))

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
    ((:on-receive callbacks) {:webmention {:id id :source s :target t}})
    ;; check if such webmention exists
    (if-let [wm-data ((:get storage) s t)]
      ;; if so, update it or delete it
      (process-existing-webmention wm-data callbacks storage config)
      ;; if not, write to data
      (process-new-webmention id s t callbacks storage config))))

(defn start
  "Start a go-loop that recursively calls to process-webmention."
  [wmc callbacks storage config]
  (go-loop [wm (<! wmc)]
    (timbre/info "webmention found in queue, processing.")
    (process-webmention wm callbacks storage config)
    (recur (<! wmc))))

