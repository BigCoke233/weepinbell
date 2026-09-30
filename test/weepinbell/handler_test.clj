(ns weepinbell.handler-test
  (:require [clojure.test :refer [testing is deftest]]
            [ring.mock.request :as mock]
            [weepinbell.handler :refer [app]]
            [cheshire.core :refer [parse-string]]))

(defmacro test-mention-list-by-domain-route [mftype]
  `(testing (str "/api/" ~mftype "/:domain route")
     (let [~'response ((app nil {:get-by-domain (fn [~'_]
                                                  [{:webmention {:source ""
                                                                 :target ""
                                                                 :id ""
                                                                 :modified 0}}])}
                            {})
                       (mock/request :get (str "/api/" ~mftype "/www.example")))]
       (is (= (:status ~'response) 200))
       (is (some? (:body ~'response))))))

(defmacro test-mention-list-by-url-route [mftype]
  `(testing (str "/api/" ~mftype "?url= route")
     (let [~'response ((app nil 
                            {:get-by-domain
                             (fn [~'_]
                                [{:webmention {:source "https://source.net"
                                               :target "http://localhost"
                                               :id "123456789123456789"
                                               :modified 0}
                                  :microformats {:type :like-of}}])}
                            {})
                       (mock/request :get (str "/api/" ~mftype "?url=http://localhost")))]
       (is (= (:status ~'response) 200))
       (is (some? (:body ~'response)))
       (when-not (-> ~'response :body parse-string empty?)
         (is (-> ~'response :body parse-string first (get "webmention") some?))))))

(deftest api-routes-test
  (test-mention-list-by-domain-route "mentions")
  (test-mention-list-by-domain-route "likes")
  (test-mention-list-by-domain-route "reposts")
  (test-mention-list-by-domain-route "replies")

  (test-mention-list-by-url-route "mentions")
  (test-mention-list-by-url-route "likes")
  (test-mention-list-by-url-route "reposts")
  (test-mention-list-by-url-route "replies")

  (testing "/api/mention/:id"
    (let [response ((app nil {:get (fn [_]
                                     {:webmention {:source ""
                                                   :target ""
                                                   :id "xxx"
                                                   :modified 0}})}
                         {})
                    (mock/request :get "/api/mention/xxx"))]
      (is (= (:status response) 200))))

  (testing "/api/mention/?source=&target="
    (let [response ((app nil {:get (fn [_ _]
                                     {:webmention {:source "1"
                                                   :target "2"
                                                   :id "xxx"
                                                   :modified 0}})}
                         {})
                    (mock/request :get "/api/mention/?source=1&?target=2"))]
      (is (= (:status response) 200)))))

(deftest feed-route-test
  (testing "RSS feed for allowed domains and has items"
    (let [response ((app nil {:get-by-domain (fn [_] [{:webmention nil}])}
                         {:allowed-target-domains "valid.com"})
                    (mock/request :get "/feed/valid.com"))]
      (is (= (:status response) 404))))
  (testing "RSS feed for allowed domains, but with no webmentions"
    (let [response ((app nil nil {:allowed-target-domains "valid.com"})
                    (mock/request :get "/feed/valid.com"))]
      (is (= (:status response) 404))))
  (testing "not found for disallowed domains"
    (let [response ((app nil nil {}) (mock/request :get "/feed/invalid.com"))]
      (is (= (:status response) 404)))))

(deftest not-found-route-test
  (testing "not-found route"
    (let [response ((app nil nil {}) (mock/request :get "/invalid"))]
      (is (= (:status response) 404))))
  (testing "root route is not found when frontend is disabled"
    (let [response ((app nil nil {:frontend nil}) (mock/request :get "/"))]
      (is (= (:status response) 404)))))

(deftest frontend-route-test
  (testing "serve frontend resources from disk when enabled"
    (let [dir (System/getProperty "java.io.tmpdir")
          response ((app nil nil {:frontend {:source :disk :dir dir}})
                    (mock/request :get "/"))]
      (is (not= (:status response) 500))))
  (testing "serve nothing when disabled"
    (let [response ((app nil nil {:frontend nil})
                    (mock/request :get "/"))]
      (is (= (:status response) 404)))))

