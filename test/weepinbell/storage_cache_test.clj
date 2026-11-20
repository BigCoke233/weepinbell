(ns weepinbell.storage-cache-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.storage.cache :refer [cache]]))

(deftest cache-hit-test
  (testing "If a query was cached, it should not hit database query."
    (let [dummy {:webmention {:id ""}}
          get-counter (agent 0)
          get-by-domain-counter (agent 0)
          base-storage {:append (fn [_])
                        :get-by-domain 
                        (fn [& _]
                          (send get-by-domain-counter inc)
                          [dummy])
                        :get (fn [& _]
                               (send get-counter inc)
                               dummy)
                        :update (fn [& _])
                        :delete (fn [& _])}
          cached-storage (cache base-storage)]
      (dotimes [_ 10] ((:get cached-storage) ""))
      (dotimes [_ 10] ((:get-by-domain cached-storage) ""))
      (is (= 1 @get-counter))
      (is (= 1 @get-by-domain-counter)))))
