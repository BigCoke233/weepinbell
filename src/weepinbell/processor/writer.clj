(ns weepinbell.processor.writer
  (:require [taoensso.timbre :as timbre]))

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
      (-> data (assoc :ok? true)))))
