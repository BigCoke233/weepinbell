(ns weepinbell.storage-test
  (:require [clojure.test :refer [testing is deftest]]
            [weepinbell.storage.edn-storage :as edns]
            [clojure.spec.alpha :as s]
            [weepinbell.storage.spec]))

(def spec-check (partial s/valid? :weepinbell.storage.spec/vtable))
(deftest storage-conformity-test
  (testing "edn-storage should conform storage spec."
    (with-redefs [weepinbell.storage.edn-storage/file-exists? (fn [_] true)]
      (is (spec-check (edns/use-store "source.edn")))))
  (testing "spec-check invalid storage vtables"
    (is (not (spec-check {:get-by-domain (fn [_])})))
    (is (not (spec-check {:append (fn [_]) :get (fn [_])})))
    (is (not (spec-check nil)))
    (is (not (spec-check 1)))
    (is (not (spec-check "I am storage. Trust me.")))
    (is (not (spec-check [])))
    (is (not (spec-check {})))))
