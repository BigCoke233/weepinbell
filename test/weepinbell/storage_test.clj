(ns weepinbell.storage-test
  (:require [clojure.test :refer [testing is deftest]]
            [weepinbell.storage.edn-storage :as edns]
            [clojure.spec.alpha :as s]
            [weepinbell.utils]
            [weepinbell.spec]))

(deftest edn-storage-conformity-test 
  (testing "edn-storage should conform storage spec."
    (with-redefs [weepinbell.utils/file-exists? (fn [_] true)]
      (is (s/valid? :weepinbell.spec/storage 
                    (edns/use-store "source.edn"))))))
