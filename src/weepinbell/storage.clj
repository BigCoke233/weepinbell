(ns weepinbell.storage
  (:require [clojure.edn :as edn]
            [weepinbell.config :refer [get-config]]))

(def data-file (get-config :data-path))

(defn load-data
  "Read data file and return data as clojure data sturcture, a vector containing Webmention maps in this case."
  []
  (-> data-file slurp edn/read-string))

(defn unload-data
  "Write to data file, replacing everything."
  [data]
  (spit data-file (pr-str data)))

(defn append
  "Append an accepted webmention to .edn data file."
  [data]
  (spit data-file (pr-str data) :append true))

(defn delete
  "Delete a Webmention entry by source and target."
  [source target]
  (let [data (load-data)
        pred #(and (= source (:wm-source %))
                   (= target (:wm-target %)))]
    (->> data
         (remove pred)
         vec
         unload-data)))

