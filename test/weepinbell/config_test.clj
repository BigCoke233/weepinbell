(ns weepinbell.config-test
  (:require [weepinbell.config :refer [load-config]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]))

(deftest config-load-test
  (testing "Config file merged with default config"
    (let [external-config {:port 4000}
          config-file "tmp/config.tmp.edn"]
      (io/make-parents config-file)
      (spit config-file external-config)
      (let [config (load-config config-file)]
        (is (= (:port config) (:port external-config)))
        (is (= (:endpoint config) "/endpoint")))
      (io/delete-file config-file))))

