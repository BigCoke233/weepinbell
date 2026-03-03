(ns weepinbell.storage
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [weepinbell.config :refer [get-config]]))

(defn append
  "Append an accepted webmention to .edn data file."
  [data]
  (let [filename (get-config :data-path)]
    (spit filename (pr-str data) :append true)))

