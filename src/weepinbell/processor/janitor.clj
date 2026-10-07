(ns weepinbell.processor.janitor
  (:require [taoensso.timbre :as timbre]))

(defn clean
  [data storage callbacks]
  (when (:ok? data)
    (let [{:keys [verified? new? id old-data]} data]
      (if (and (not new?) (not verified?))
        (do ;; delete old unverified webmention 
          ((:delete storage) id)
          ((:on-delete callbacks) old-data)
          (timbre/warn "webmention" id "no longer valid, deleted.")
          (-> data (assoc :ok? false)))
        (-> data (assoc :ok? true))))))
