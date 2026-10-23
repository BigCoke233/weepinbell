(ns weepinbell.migration
  (:require [weepinbell.storage.h2 :as h2]
            [weepinbell.storage.edn-storage :as edns])
  #_(:import [org.h2 SQLiteException]))

(defn edn->h2
  "Given a EDN file and a SQLite DB path, migrate data from that
   edn file to SQLite DB. It appends data to DB so the destination
   db can be empty or it can has some data."
  [edn-file h2-db]
  {:pre [(some? edn-file) (some? h2-db)]}
  (let [ds (h2/make-datasource h2-db)]
    (h2/create-tables ds)
    (edns/with-webmentions edn-file
      (fn [wms]
        (doseq [wm wms]
          (let [id (get-in wm [:webmention :id])]
            (println "Appending webmention" id "to database")
            (try 
              (h2/insert-webmention ds wm)
;;              (catch SQLiteException e
;;                (println "Something wrong with data or database")
;;                (println (.getMessage e)))
              (catch Exception e
                (println "Something unexpected happened.")
                (println (.getMessage e))))))))
    (println "Done")))
