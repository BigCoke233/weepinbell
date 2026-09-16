(ns weepinbell.webhook-test
  (:require [clojure.test :refer [testing is deftest]]
            [org.httpkit.server :refer [run-server]]
            [weepinbell.webhook :as webhook]))

(def test-wm-data {:webmention {:target "https://target.net/"
                                :source "https://source.net/"}
                   :extra {:proxied-photo "https://test.com/photo.png"}})

(deftest webhook-test
  (testing "Test webhooks triggers with ntfy request"
    (let [received (promise)
          payload-ok (promise)
          handler  (fn [req]
                     (deliver received true)
                     (let [headers (:headers req)
                           title (get headers "title")
                           icon (get headers "icon")
                           click (get headers "click")]
                       (when (and (= title "source.net mentioned you!")
                                  (= click "https://source.net/")
                                  (= icon "https://test.com/photo.png"))
                         (deliver payload-ok true)))
                     {:status 200 :headers {} :body "ok"})
          server   (run-server handler {:port 18888 :join? false})]
      (webhook/trigger :on-create
                       [{:url "http://127.0.0.1:18888/webhook-test"
                         :request-fn :ntfy}]
                       test-wm-data)
      (is (deref received 2000 nil))
      (is (deref payload-ok 2000 nil))
      (server))))

(def test-make (partial webhook/make-url :on-create test-wm-data))
(deftest make-url-test
  (testing "Test webhook replacing dynamic parameters in URL"
    (is (= (test-make "https://webhook.test/{event}")
           "https://webhook.test/on-create"))
    (is (= (test-make "https://webhook.test/{source-domain}_mentioned_you")
           "https://webhook.test/source.net_mentioned_you"))
    (is (= (test-make "https://webhook.test/{source-encoded}/{target-encoded}")
           "https://webhook.test/https%3A%2F%2Fsource.net%2F/https%3A%2F%2Ftarget.net%2F"))
    (is (= (test-make "https://hook.{target-domain}/update")
           "https://hook.target.net/update"))
    (is (= (test-make "https://webhook.test?=source={source}&target={target}")
           "https://webhook.test?=source=https://source.net/&target=https://target.net/"))))

(deftest make-request-test
  (testing "Test webhook constructing request options dynamically"
    (let [test-data {:webmention {:target "https://target.net/"
                                  :source "https://source.net/"}
                     :extra {:proxied-photo "https://test.com/photo.png"}}]
      (is (= (webhook/make-request :on-create test-data :ntfy)
             {:body "from https://source.net/\nto https://target.net/"
              :headers {"Title" "source.net mentioned you!"
                        "Click" "https://source.net/"
                        "Icon" "https://test.com/photo.png"}})))))
