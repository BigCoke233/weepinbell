(ns weepinbell.processor-starter-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.core.async :refer [chan put!]]
            [weepinbell.processor.facade :refer [start-processor]]))

(defn- test-processor [test-wmc received?]
  {:pre [(instance? clojure.lang.IPending received?)]}
  (let [pmeta {:wmc test-wmc
               :callbacks {:on-receive (fn [_] (deliver received? true))
                           :on-create (fn [_])
                           :on-delete (fn [_])
                           :on-update (fn [_])}
               :storage {:get (fn [& _])
                         :delete (fn [& _])
                         :append (fn [& _])
                         :update (fn [& _])
                         :get-by-domain (fn [& _])}
               :config {:base-url "https://test.com"
                        :allowed-target-domains ["test.com"]
                        :pipeline-concurrency 1}}]
    (start-processor pmeta)))

(deftest start-processor-test
  (testing "graceful shutdown"
    (let [test-wmc (chan)
          received? (promise)
          shutdown (test-processor test-wmc received?)]
      (shutdown)
      (put! test-wmc {:s "s" :t "t"})
      (is (not (deref received? 50 false)))))
  (testing "processor starting and taking webmention"
    (let [test-wmc (chan)
          received? (promise)
          shutdown (test-processor test-wmc received?)]
      (put! test-wmc {:s "s" :t "t"})
      (is (deref received? 50 false))
      (shutdown))))

