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

(defn get-wm
  "Get an existing Webmention by source and target."
  [source target]
  (let [data (load-data)
        pred #(and (= source (:wm-source %))
                   (= target (:wm-target %)))]
    (first (filter pred data))))

(defn with-this-wm
  "Do something with this Webmention (identified by source and target)."
  [source target do-something-with]
  (when-let [this-wm (get-wm source target)]
    (do-something-with this-wm)))

(defn append
  "Append an accepted webmention to .edn data file."
  [data]
  (spit data-file (pr-str data) :append true))

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

