(ns weepinbell.storage-test
  (:require [clojure.test :refer [testing is deftest]]
            [weepinbell.storage.edn-storage :as edns]
            [clojure.spec.alpha :as s]
            [weepinbell.storage.edn-storage]
            [weepinbell.storage.spec]))

(deftest edn-storage-conformity-test
  (testing "edn-storage should conform storage spec."
    (with-redefs [weepinbell.storage.edn-storage/file-exists? (fn [_] true)]
      (is (s/valid? :weepinbell.storage.spec/vtable
                    (edns/use-store "source.edn"))))))
