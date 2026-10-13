#!/usr/bin/env bb

;; This is a Babashka script that recalculates
;; all :webmention :id in a edn data file 
;;
;; this is meant to be used in these scenarios:
;; 1. migarting from before v4.1.0, where :id used to be 
;;    MD5 hashes instead of sha-256 and needs to be 
;;    recalculated to SHA-256 
;; 2. migrating from before v0.2.0, where :id was not 
;;    introduced yet

(require '[clojure.edn :as edn])
(require '[clojure.string :refer [upper-case join]])
(require '[clojure.java.io :as io])
(import java.security.MessageDigest)

;; copied from production code 
;; version: commit e00891a5e7
(defn with-webmentions
  "Opens the data file and applies function f to a lazy sequence of parsed Webmentions."
  [datasource f]
  (with-open [r (io/reader datasource)]
    (->> (line-seq r)
         (map edn/read-string)
         f)))
(defn- java-digest [^String s algo]
  (let [algorithm (MessageDigest/getInstance algo)
        raw-bytes (.digest algorithm (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) raw-bytes))))
(defn digest
  ([s]
   (digest s :sha-256 32))
  ([s algo]
   (digest s algo 32))
  ([s algo length]
   (->> (java-digest s (upper-case (name algo)))
        (take length)
        (apply str))))
(defn unload-data
  "Write to data file, replacing everything."
  [datasource data]
  (when-let [data-with-string (map pr-str data)]
    (spit datasource (join "\n" data-with-string))))
;; end 

(let [[datafile] *command-line-args*]
  (with-webmentions datafile
    (fn [wms]
      (println "starting reading, total count:" (count wms))
      (->> wms
           (map (fn [wm]
                  (let [webmention (:webmention wm)
                        source (:source webmention)
                        target (:target webmention)
                        new-id (digest (str source target))]
                    (println new-id)
                    (assoc wm :webmention (assoc webmention :id new-id)))))
           (unload-data datafile))
      (println "complete!"))))
