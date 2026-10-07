(ns weepinbell.processor.storage-checker
  (:require [taoensso.timbre :as timbre]))

(defn check-storage
  ;; data: {:id ..., :wm {:s ... :t ...}, :ok? true}
  [data storage]
  (when (:ok? data)
    (timbre/info "checking if webmention" (:id data) "already exists")
    (let [{:keys [id wm]} data
          {:keys [s t]} wm
          old-data (or ((:get storage) id)
                       ((:get storage) s t))]
      (-> data
          (assoc :old-data old-data)
          (assoc :new? (or (nil? old-data) (empty? old-data)))
          (assoc :ok? true)))))
