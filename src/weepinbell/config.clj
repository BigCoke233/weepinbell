(ns weepinbell.config
  "Config loader component that merges default config with user config.
  Default config is located at resources/config.edn"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load-config []
  (let [default-config (when-let [resource (io/resource "config.edn")]
                         (-> resource slurp edn/read-string))
        external-file (io/file "config.edn")
        external-config (when (.exists external-file)
                          (-> external-file slurp edn/read-string))]
    (merge default-config external-config)))

(def config (delay (load-config)))

(defn get-config [key & keys]
  (get-in @config (cons key keys)))

