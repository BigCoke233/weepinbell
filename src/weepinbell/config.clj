(ns weepinbell.config
  "Config loader component that merges default config with user config.
  Default config is located at resources/config.edn"
  (:require [fast-edn.core :as edn]
            [clojure.java.io :as io]))

(defn load-config [config-file]
  (let [default-config (when-let [resource (io/resource "config.edn")]
                         (-> resource slurp edn/read-string))
        external-file (io/file config-file)
        external-config (when (.exists external-file)
                          (-> external-file slurp edn/read-string))]
    (merge default-config external-config)))

