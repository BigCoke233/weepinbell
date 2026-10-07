(ns weepinbell.processor.writer
  (:require [taoensso.timbre :as timbre]))

(defn write
  [data storage callbacks]
  (when (:ok? data)
    (let [{:keys [id new? new-data]} data]
      (if new?
        (do ;; append new webmention
          ((:append storage) new-data)
          ((:on-create callbacks) new-data)
          (timbre/info "new webmention" id "received."))
        (do ;; update old webmention
          ((:update storage) id new-data)
          ((:on-update callbacks) new-data)
          (timbre/info "webmention" id "updated.")))
      (-> data (assoc :ok? true)))))
