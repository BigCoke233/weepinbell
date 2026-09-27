(ns weepinbell.webhook-test
  (:require [clojure.test :refer [testing is deftest]]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.params :refer [wrap-params]]
            [weepinbell.webhook :as webhook]))

(def test-wm-data {:webmention {:target "https://target.net/"
                                :source "https://source.net/"}
                   :extra {:proxied-photo "https://test.com/photo.png"}})

(deftest webhook-test
  (testing "Test webhooks triggers with bark request"
    (let [received (promise)
          payload-ok (promise)
          handler  (fn [req]
                     (deliver received true)
                     (let [params (:form-params req)
                           title (get params "title")
                           icon (get params "icon")
                           url (get params "url")]
                       (when (and (= title "source.net mentioned you!")
                                  (= url "https://source.net/")
                                  (= icon "https://test.com/photo.png"))
                         (deliver payload-ok true)))
                     {:status 200 :headers {} :body "ok"})
          server   (run-server (wrap-params handler) {:port 18888 :join? false})]
      (webhook/trigger :on-create
                       [{:url "http://127.0.0.1:18888/webhook-test"
                         :request-fn :bark}]
                       test-wm-data)
      (is (deref received 2000 nil))
      (is (deref payload-ok 2000 nil))
      (server)))
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
      (server)))
  (testing "Webhook handle invalid URLs"
    (let [received (promise)
          handler (fn [_] (deliver received true))
          server (run-server handler {:port 18888 :join? false})]
      (webhook/trigger :on-update ["test"] test-wm-data)
      (is (not (deref received 100 false)))
      (server))))

(def test-make (partial webhook/make-url :on-create test-wm-data))
(def test-make-empty (partial webhook/make-url :on-delete {}))
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
           "https://webhook.test?=source=https://source.net/&target=https://target.net/")))
  (testing "Dynamic URL parameters with empty data"
    (is (= (test-make-empty "{source}{target}{source-encoded}{target-encoded}{source-domain}{target-domain}{photo}{event}")
           "on-delete"))))

(deftest make-request-test
  (testing "Webhook constructing request options dynamically with presets"
    (is (= (webhook/make-request :on-create test-wm-data :ntfy)
           {:body "from https://source.net/\nto https://target.net/"
            :headers {"Title" "source.net mentioned you!"
                      "Click" "https://source.net/"
                      "Icon" "https://test.com/photo.png"}})))
  (testing "Webhook constructing request options with custom functions"
    (let [custom-map {:body "customized!"}]
      (is (= (webhook/make-request :on-receive test-wm-data (fn [& _] custom-map))
             custom-map)))))

(deftest make-actions-test
  (testing "Constructing action map"
    (is (= (webhook/make-actions :default ["https://test.com"
                                           {:url "https://test.net" :request-fn :test-fn}])
           [{:url "https://test.com" :request-fn :default}
            {:url "https://test.net" :request-fn :test-fn}])))
  (testing "Constructing action map with empty data"
    (is (= (webhook/make-actions nil nil) []))
    (is (= (webhook/make-actions :default []) []))))

(deftest make-hooks-test 
  (testing "Still need all four event keys even if config has less than four. (:on-create, :on-delete, :on-receive, :on-update)"
    (let [config {:webhook {:on-create ["https://test.url/"]}}
          hooks (webhook/make-hooks config)
          hook-keys (set (keys hooks))
          expected (set [:on-create :on-delete :on-receive :on-update])]
      (is (= hook-keys expected)))))

