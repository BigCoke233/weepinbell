(ns weepinbell.storage
  (:require [clojure.edn :as edn]
            [weepinbell.config :refer [get-config]]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]))

(def data-file (get-config :data-path))

(defn load-data
  "Read data file and return data as clojure data sturcture, a vector containing Webmention maps in this case."
  []
  (with-open [r (io/reader data-file)]
    (let [string-vector (-> r line-seq vec)]
      (vec (map edn/read-string string-vector)))))

(defn unload-data
  "Write to data file, replacing everything."
  [data]
  (when-let [data-with-string (map pr-str data)]
    (spit data-file (join "\n" data-with-string))))

(defn get-wm
  "Get a Webmention by source/target or by ID."
  ([id]
   (get-wm id nil))
  ([source target]
   (with-open [r (io/reader data-file)]
     (let [lines (line-seq r)
           pred (if target
                  (fn [wm] (and (= source (get-in wm [:webmention :source]))
                                (= target (get-in wm [:webmention :target]))))
                  (fn [wm] (= source (get-in wm [:webmention :id]))))]
       (->> lines
            (map edn/read-string)
            (filter pred)
            first)))))

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

(defn delete-wm
  "Delete a Webmention entry by source and target."
  [source target]
  (when-let [wm (get-wm source target)]
     (->> (load-data)
       (remove #(= wm %))
       unload-data)))

(defn update-wm
  "Update an existing webmention"
  [source target new-data]
  (delete-wm source target)
  (append-wm new-data))

