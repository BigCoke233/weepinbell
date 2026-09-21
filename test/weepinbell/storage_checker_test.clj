(ns weepinbell.storage-checker-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.processor.storage-checker :refer [check-storage]]))

(deftest process-handler-storaga-checker-test
  (testing "nil when not OK"
    (is (nil? (check-storage {:ok? false} {}))))
  (testing "no such webmention stored"
    (let [data {:ok? true
                :id "1234"
                :wm {:s "s" :t "t"}}]
      (is (= (check-storage data {:get (fn [& _] [])})
             (-> data
                 (assoc :old-data [])
                 (assoc :new? true)
                 (assoc :ok? true))))))
  (testing "webmention exists"
    (let [data {:ok? true
                :id "1234"
                :wm {:s "s" :t "t"}}]
      (is (= (check-storage data {:get (fn [& _] [{}])})
             (-> data
                 (assoc :old-data [{}])
                 (assoc :new? false)
                 (assoc :ok? true)))))))

