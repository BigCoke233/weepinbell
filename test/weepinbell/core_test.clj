(ns weepinbell.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.core :refer [start-system]]
            [weepinbell.config :refer [load-config]]))

(deftest start-system-test
  (testing "system starts and shuts down normally"
    (let [shutdown (start-system (load-config "config.edn"))]
      (is (fn? shutdown))
      (is (true? (shutdown))))))
