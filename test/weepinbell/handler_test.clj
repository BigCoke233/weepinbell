(ns weepinbell.handler-test
  (:require [clojure.test :refer [testing is deftest]]
            [ring.mock.request :as mock]
            [weepinbell.webserver.handler :refer [app]]
            [weepinbell.webserver.facade :refer [make-services]]
            [cheshire.core :refer [parse-string]]))

(defn test-app [wmc store config]
  (app {:wmc wmc :store store :config config 
        :services (make-services)}))

(deftest api-routes-test
  (testing "/api/mentions/:domain route"
     (let [response ((test-app nil {:get-by-domain (fn [_]
                                                  [{:webmention {:source ""
                                                                 :target ""
                                                                 :id ""
                                                                 :modified 0}}])}
                            {})
                       (mock/request :get "/api/mentions/www.example"))]
       (is (= (:status response) 200))
       (is (some? (:body response)))))

  (testing "/api/mentions?url= route"
     (let [response ((test-app nil 
                            {:get-by-domain
                             (fn [_]
                                [{:webmention {:source "https://source.net"
                                               :target "http://localhost"
                                               :id "123456789123456789"
                                               :modified 0}
                                  :microformats {:type :like-of}}])}
                            {})
                       (mock/request :get "/api/mentions?url=http://localhost"))]
       (is (= (:status response) 200))
       (is (some? (:body response)))
       (when-not (-> response :body parse-string empty?)
         (is (-> response :body parse-string first (get "webmention") some?)))))

  (testing "/api/count/:domain route"
     (let [response ((test-app nil {:get-by-domain (fn [_]
                                                  [{:webmention {:source ""
                                                                 :target ""
                                                                 :id ""
                                                                 :modified 0}}])}
                            {})
                       (mock/request :get "/api/count/www.example.com"))]
       (is (= (:status response) 200))
       (is (= 1 (-> response :body parse-string (get "count") Long.)))))

  (testing "/api/count?url= route"
     (let [response ((test-app nil 
                            {:get-by-domain
                             (fn [_] [])}
                            {})
                       (mock/request :get "/api/count?url=http://localhost"))]
       (is (= (:status response) 200))
       (is (= 0 (-> response :body parse-string (get "count") Long.)))))

  (testing "/api/mention/:id"
    (let [response ((test-app nil {:get (fn [_]
                                     {:webmention {:source ""
                                                   :target ""
                                                   :id "xxx"
                                                   :modified 0}})}
                         {})
                    (mock/request :get "/api/mention/xxx"))]
      (is (= (:status response) 200))))

  (testing "/api/mention/?source=&target="
    (let [response ((test-app nil {:get (fn [_ _]
                                     {:webmention {:source "1"
                                                   :target "2"
                                                   :id "xxx"
                                                   :modified 0}})}
                         {})
                    (mock/request :get "/api/mention/?source=1&?target=2"))]
      (is (= (:status response) 200)))))

(deftest feed-route-test
  (testing "RSS feed for allowed domains and has items"
    (let [response ((test-app nil {:get-by-domain (fn [_] [{:webmention nil}])}
                         {:allowed-target-domains "valid.com"})
                    (mock/request :get "/feed/valid.com"))]
      (is (= (:status response) 404))))
  (testing "RSS feed for allowed domains, but with no webmentions"
    (let [response ((test-app nil nil {:allowed-target-domains "valid.com"})
                    (mock/request :get "/feed/valid.com"))]
      (is (= (:status response) 404))))
  (testing "not found for disallowed domains"
    (let [response ((test-app nil nil {}) (mock/request :get "/feed/invalid.com"))]
      (is (= (:status response) 404)))))

(deftest not-found-route-test
  (testing "not-found route"
    (let [response ((test-app nil nil {}) (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

