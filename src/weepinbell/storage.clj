(ns weepinbell.storage
  (:require [clojure.edn :as edn]
            [weepinbell.config :refer [get-config]]
            [weepinbell.utils :refer [get-domain get-target get-path]]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]))

(def data-file (get-config :data-path))

(defn load-data
  "Read data file and return data as clojure data sturcture, a vector containing Webmention maps in this case. Note that is't extremely costly to load all data at once!"
  []
  (with-open [r (io/reader data-file)]
    (let [string-vector (-> r line-seq vec)]
      (vec (map edn/read-string string-vector)))))

(defn unload-data
  "Write to data file, replacing everything."
  [data]
  (when-let [data-with-string (map pr-str data)]
    (spit data-file (join "\n" data-with-string))))

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
  [f]
  (with-open [r (io/reader data-file)]
    (->> (line-seq r)
         (map edn/read-string)
         f)))

(defn get-wm
  ([id] (get-wm id nil))
  ([source target]
   (let [pred (get-predicate source target)]
     (with-webmentions
       #(first (filter pred %))))))

(defn delete-wm
  ([id] (delete-wm id nil))
  ([source target]
   (let [pred (get-predicate source target)]
     (with-webmentions
       #(->> % (remove pred) unload-data)))))

(defn get-wms-by-domain
  ([domain]
    (get-wms-by-domain domain nil))
  ([domain path]
   (with-webmentions
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
  [data]
  (let [newline? (if (first-line-empty? data-file) "" "\n")
        line (str newline? (pr-str data))]
    (spit data-file line :append true)))

(defn update-wm
  "Update an existing webmention"
  [source target new-data]
  (delete-wm source target)
  (append-wm new-data))

