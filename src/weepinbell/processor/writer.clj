(ns weepinbell.processor.writer
  (:require [taoensso.timbre :as timbre]
            [weepinbell.storage.spec]
            [clojure.spec.alpha :as s]))

(defn write
  [data storage callbacks]
  (when (:ok? data)
    (let [{:keys [id new? new-data]} data]
      (if new?
        (do ;; append new webmention
          (timbre/info "new webmention" id "processed, writing to storage...")
          ((:append storage) new-data)
          ((:on-create callbacks) new-data))
        (do ;; update old webmention
          (timbre/info "webmention" id "exists, updating...")
          ((:update storage) id new-data)
          ((:on-update callbacks) new-data)))
      (when (not (s/valid? :weepinbell.storage.spec/webmention-wrapped new-data))
        (timbre/warn "webmention" id "data does not match spec before writing to storage"))
      (-> data (assoc :ok? true)))))
