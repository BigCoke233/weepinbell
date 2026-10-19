(ns weepinbell.migration
  (:require [weepinbell.storage.sqlite :as sqlite]
            [weepinbell.storage.edn-storage :as edns]))

(defn edn->sqlite [edn-file sqlite-db]
  {:pre [(some? edn-file) (some? sqlite-db)]}
  (let [ds (sqlite/make-datasource sqlite-db)]
    (sqlite/create-tables ds)
    (edns/with-webmentions edn-file
      (fn [wms]
        (doseq [wm wms]
          (let [id (get-in wm [:webmention :id])]
            (println "Appending webmention" id "to database")
            (sqlite/insert-webmention ds wm)))))
    (println "Done")))
