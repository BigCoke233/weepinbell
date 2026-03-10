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
  "Get an existing Webmention by source and target."
  [source target]
  (let [data (load-data)
        pred #(and (= source (get-in % [:webmention :source]))
                   (= target (get-in % [:webmention :target])))]
    (first (filter pred data))))

(defn with-this-wm
  "Do something with this Webmention (identified by source and target)."
  [source target do-something-with]
  (when-let [this-wm (get-wm source target)]
    (do-something-with this-wm)))

(defn first-line-empty? [path]
  (with-open [rdr (io/reader path)]
    (let [line (line-seq rdr)]
      (empty? line))))

(defn append
  "Append an accepted webmention to .edn data file."
  [data]
  (let [newline? (if (first-line-empty? data-file) "" "\n")
        line (str newline? (pr-str data))]
    (spit data-file line :append true)))

(defn update-wm
  "Update an existing webmention"
  [source target new-data]
  #_(todo))

(defn delete-wm
  "Delete a Webmention entry by source and target."
  [source target]
  (with-this-wm source target
    (fn [this-wm]
      (->> (load-data)
           (remove #(= this-wm %))
           vec
           unload-data))))

