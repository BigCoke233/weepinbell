(ns weepinbell.janitor-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.processor.janitor :refer [clean]]))

(def old-unverified {:ok? true
                     :verified? false
                     :new? false
                     :id "1234"
                     :old-data {}})
(def new-unverified {:ok? true
                     :verified? false
                     :new? true
                     :id "1234"
                     :old-data {}})
(def old-verified {:ok? true
                   :verified? true
                   :new? false
                   :id "1234"
                   :old-data {}})
(def new-verified {:ok? true
                   :verified? true
                   :new? true
                   :id "1234"
                   :old-data {}})

(defn- not-if [condition value]
  (if condition (not value) value))

(defn- test-clean [testdata should-clean?]
  (let [deleted (promise)
        storage {:delete (fn [_] (deliver deleted true))}
        callbacked (promise)
        callbacks {:on-delete (fn [_] (deliver callbacked true))}
        cleaned (clean testdata storage callbacks)]
    (is (not-if should-clean? (:ok? cleaned)))
    (is (not-if should-clean? (not (deref deleted 10 false))))
    (is (not-if should-clean? (not (deref callbacked 10 false))))))

(deftest janitor-test
  (testing "janitor cleans old unverified webmention"
    (test-clean old-unverified true))
  (testing "janitor does nothing for new unverified and all verified webmentions"
    (doall (map #(test-clean % false) [old-verified new-verified new-unverified]))))
