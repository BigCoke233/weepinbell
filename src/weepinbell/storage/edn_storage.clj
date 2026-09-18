(ns weepinbell.storage.edn-storage
  "EDN file-based storage middle layer.
  Entry function is use-store, which makes a valid storage vtable."
  (:require [clojure.edn :as edn]
            [weepinbell.utils :refer [file-exists?]]
            [weepinbell.utils.url :refer [get-domain get-target get-path]]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]))

(defn unload-data
  "Write to data file, replacing everything."
  [datasource data]
  (when-let [data-with-string (map pr-str data)]
    (spit datasource (join "\n" data-with-string))))

;; predicates used for filtering webmention
(defn- st-pair-equal-pred [source target wm]
  (and (= source (get-in wm [:webmention :source]))
       (= target (get-in wm [:webmention :target]))))
(defn- id-equal-pred [id wm]
  (= id (get-in wm [:webmention :id])))
(defn- get-predicate [source target]
  (if target
    #(st-pair-equal-pred source target %)
    #(id-equal-pred source %)))
(defn- domain-equal [domain wm]
  (= domain
     (-> wm get-target get-domain)))
(defn- path-equal [path wm]
  (= path
     (-> wm get-target get-path)))
;; end predicates

(defn with-webmentions
  "Opens the data file and applies function f to a lazy sequence of parsed Webmentions."
  [datasource f]
  (with-open [r (io/reader datasource)]
    (->> (line-seq r)
         (map edn/read-string)
         f)))

(defn get-wm
  ([datasource id] (get-wm datasource id nil))
  ([datasource source target]
   (let [pred (get-predicate source target)]
     (with-webmentions datasource
       #(first (filter pred %))))))

(defn delete-wm
  ([datasource id] (delete-wm datasource id nil))
  ([datasource source target]
   (let [pred (get-predicate source target)]
     (with-webmentions datasource
       #(->> %
             (remove pred)
             (unload-data datasource))))))

(defn get-wms-by-domain
  ([datasource domain]
   (get-wms-by-domain datasource domain nil))
  ([datasource domain path]
   (with-webmentions datasource
     #(cond->> %
        true         (filter (partial domain-equal domain))
        (some? path) (filter (partial path-equal path))
        true         vec))))

(defn first-line-empty? [path]
  (with-open [rdr (io/reader path)]
    (let [line (line-seq rdr)]
      (empty? line))))

(defn append-wm
  "Append an accepted webmention to .edn data file."
  [datasource data]
  (let [newline? (if (first-line-empty? datasource) "" "\n")
        line (str newline? (pr-str data))]
    (spit datasource line :append true)))

(defn update-wm
  "Update an existing webmention"
  [datasource source target new-data]
  (delete-wm datasource source target)
  (append-wm datasource new-data))

;; make storage vtable
(defn use-store [datasource]
  (when (not (file-exists? datasource))
    (io/make-parents datasource)
    (spit datasource ""))
  {:append (partial append-wm datasource)
   :update (partial update-wm datasource)
   :delete (partial delete-wm datasource)
   :get    (partial get-wm datasource)
   :get-by-domain (partial get-wms-by-domain datasource)})

