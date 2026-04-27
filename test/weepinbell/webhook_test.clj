(ns weepinbell.webhook-test
  (:require [clojure.test :refer [testing is deftest]]
            [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.webhook :as webhook]))

(deftest -test
  (testing "Test webhooks triggers"
    (let [received (promise)
          handler  (fn [_]
                     (deliver received true)
                     {:status 200 :headers {} :body "ok"})
          server   (run-jetty handler {:port 18888 :join? false})]
      (webhook/trigger :on-create
                       {:on-create ["http://127.0.0.1:18888/webhook-test"]})
      (is (deref received 2000 nil)
          "Webhook request was received")
      (.stop server))))
