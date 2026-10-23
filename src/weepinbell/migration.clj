(ns weepinbell.migration
  (:require [weepinbell.storage.sqlite :as sqlite]
            [weepinbell.storage.edn-storage :as edns])
  (:import [org.sqlite SQLiteException]))

(defn edn->sqlite 
  "Given a EDN file and a SQLite DB path, migrate data from that
   edn file to SQLite DB. It appends data to DB so the destination
   db can be empty or it can has some data."
  [edn-file sqlite-db]
  {:pre [(some? edn-file) (some? sqlite-db)]}
  (let [ds (sqlite/make-datasource sqlite-db)]
    (sqlite/create-tables ds)
    (edns/with-webmentions edn-file
      (fn [wms]
        (doseq [wm wms]
          (let [id (get-in wm [:webmention :id])]
            (println "Appending webmention" id "to database")
            (try 
              (sqlite/insert-webmention ds wm)
              (catch SQLiteException e
                (println "Something wrong with data or database")
                (println (.getMessage e)))
              (catch Exception e
                (println "Something unexpected happened.")
                (println (.getMessage e))))))))
    (println "Done")))
