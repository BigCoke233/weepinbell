(ns weepinbell.processor-starter-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.core.async :refer [chan put!]]
            [weepinbell.processor.starter :refer [start-processor]]))

(def test-wmc (chan))
(def received (promise))
(def processor-meta {:wmc test-wmc
                     :callbacks {:on-receive (fn [_] (deliver received true))
                                 :on-create (fn [_])
                                 :on-delete (fn [_])
                                 :on-update (fn [_])}
                     :storage {:get (fn [& _])
                               :delete (fn [& _])
                               :append (fn [& _])
                               :update (fn [& _])
                               :get-by-domain (fn [& _])}
                     :config {:base-url "https://test.com"
                              :allowed-target-domains ["test.com"]}})

(deftest start-processor-test
  (testing "starting processor and putting a fake webmention in queue"
    (start-processor processor-meta)
    (put! test-wmc {:s "s" :t "t"})
    (is (deref received 50 false))))
