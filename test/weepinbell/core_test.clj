(ns weepinbell.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.core :refer [start-system]]))

(deftest start-system-test
  (testing "system starts and shuts down normally"
    (let [shutdown (start-system)]
      (is (fn? shutdown))
      (is (true? (shutdown))))))
