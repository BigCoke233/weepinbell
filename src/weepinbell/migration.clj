(ns weepinbell.migration
  "Migration utilities that is meant to be used as a command in the -main
   function. Not to be used inside the system.
   Features may be removed when they are not needed."
  (:require [weepinbell.storage.h2 :as h2]
            [weepinbell.storage.edn-storage :as edns]
            [weepinbell.storage.spec]
            [weepinbell.metadata.microformats :refer [parse-microformats]]
            [clojure.java.io :as io]
            [fast-edn.core :as edn]))

(defn mf->jf2
  "Given a Webmention object with microformats, convert it to
   jf2, and remove original microformats."
  [wm]
  {:pre [(map? wm) (contains? wm :microformats)]}
  (-> wm
      (assoc :jf2 (parse-microformats (:microformats wm)))
      (dissoc :microformats)))

(defn edn->h2
  "Given an EDN file and a SQLite DB path, migrate data from that
   edn file to SQLite DB. It appends data to DB so the destination
   db can be empty or it can has some data.
   This also converts microformats to jf2, as the new H2 schema is
   incompatible with microformats."
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
              (let [wm (if (contains? wm :microformats)
                         (mf->jf2 wm) wm)]
                (h2/insert-webmention ds wm))
              (catch Exception e
                (println "Something unexpected happened.")
                (println (.getMessage e))))))))
    (println "Done")))

(defn edn-jf2ify
  "Given an EDN file with deprecated microformats data, converts
   microformats to jf2 and generate a new file in new format."
  [old-edn new-edn]
  {:pre [(string? old-edn) (string? new-edn)]}
  (io/make-parents new-edn)
  (.createNewFile (io/file new-edn))
  (with-open [rdr (io/reader old-edn)]
    (doseq [wm (map edn/read-string (line-seq rdr))]
      (println (get-in wm [:webmention :id]))
      (let [new-data (if (contains? wm :microformats)
                       (mf->jf2 wm) wm)]
        (spit new-edn (prn-str new-data) :append true)))))

