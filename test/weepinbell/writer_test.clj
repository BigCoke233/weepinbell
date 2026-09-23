(ns weepinbell.writer-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.processor.writer :refer [write]]))

(defn test-write [data behavior]
  (let [appended (promise)
        updated (promise)
        append-callbacked (promise)
        update-callbacked (promise)
        storage {:append (fn [& _] (deliver appended true))
                 :update (fn [& _] (deliver updated true))}
        callbacks {:on-create (fn [_] (deliver append-callbacked true))
                   :on-update (fn [_] (deliver update-callbacked true))}
        written (write data storage callbacks)]
    (is (:ok? written))
    (when (= behavior :should-append)
      (is (deref appended 10 false)
          (deref append-callbacked 10 false)))
    (when (= behavior :should-update)
      (is (deref updated 10 false)
          (deref update-callbacked 10 false)))))

(deftest writer-test
  (testing "Append new webmentin"
    (test-write {:ok? true :new? true :id "1" :new-data {}} :should-append))
  (testing "Update old webmentin"
    (test-write {:ok? true :new? false :id "1" :new-data {}} :should-update)))
