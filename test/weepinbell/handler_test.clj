(ns weepinbell.handler-test
  (:require [clojure.test :refer [testing is deftest]]
            [ring.mock.request :as mock]
            [weepinbell.handler :refer [app]]))

(defmacro test-mention-list-by-domain-route [mftype]
  `(testing (str "/api/" ~mftype "/:domain route")
    (let [~'response ((app {:get-by-domain (fn [~'_] 
                                           [{:webmention {:source ""
                                                         :target "" 
                                                         :id "" 
                                                         :modified 0}}])}
                           {}) 
                    (mock/request :get (str "/api/" ~mftype "/www.example")))]
      (is (= (:status ~'response) 200)))))

(defmacro test-mention-list-by-url-route [mftype]
  `(testing (str "/api/" ~mftype "?url= route")
    (let [~'response ((app {:get-by-domain (fn [~'_] 
                                           [{:webmention {:source ""
                                                          :target "http://localhost/" 
                                                          :id "" 
                                                          :modified 0}}])}
                           {}) 
                    (mock/request :get (str "/api/" ~mftype "?url=http://localhost")))]
      (is (= (:status ~'response) 200)))))

(deftest api-routes-test
  (testing "homepage route"
    (let [response ((app nil {}) (mock/request :get "/"))]
      (is (= (:status response) 200))))

  (test-mention-list-by-domain-route "mentions")
  (test-mention-list-by-domain-route "likes")
  (test-mention-list-by-domain-route "reposts")
  (test-mention-list-by-domain-route "replies")

  (test-mention-list-by-url-route "mentions")
  (test-mention-list-by-url-route "likes")
  (test-mention-list-by-url-route "reposts")
  (test-mention-list-by-url-route "replies")

  (testing "/api/mention/:id"
    (let [response ((app {:get (fn [_] 
                                           {:webmention {:source ""
                                                          :target "" 
                                                          :id "xxx" 
                                                          :modified 0}})}
                         {}) 
                    (mock/request :get "/api/mention/xxx"))]
      (is (= (:status response) 200))))

  (testing "/api/mention/?source=&target="
    (let [response ((app {:get (fn [_ _] 
                                           {:webmention {:source "1"
                                                          :target "2" 
                                                          :id "xxx" 
                                                          :modified 0}})}
                         {}) 
                    (mock/request :get "/api/mention/?source=1&?target=2"))]
      (is (= (:status response) 200)))))

(deftest feed-route-test
  (testing "RSS feed for allowed domains"
    (let [response ((app nil {:allowed-target-domains "valid.com"}) 
                    (mock/request :get "/feed/valid.com"))]
      (is (= (:status response) 200))))
  (testing "not found for disallowed domains"
    (let [response ((app nil {}) (mock/request :get "/feed/invalid.com"))]
      (is (= (:status response) 404))))) 

(deftest not-found-route-test
  (testing "not-found route"
    (let [response ((app nil {}) (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

