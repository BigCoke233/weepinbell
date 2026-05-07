(ns weepinbell.processor-test
  (:require [clojure.test :refer [deftest is testing]]
            [compojure.core :refer [GET routes]]
            [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.processor :as p]))

(def mock-callbacks {:on-create  (fn [])
                    :on-delete  (fn [])
                    :on-update  (fn [])
                    :on-receive (fn [])})

(def mock-storage {:append  (fn [_])
                   :update  (fn [_ _ _])
                   :delete  (fn [_ _])
                   :get     (fn [_ _])})

(deftest new-webmention-test
  (testing "Creating a new webmention."
    (let [handler (routes
                    (GET "/source" []
                      {:status 200 
                       :headers {"Content-Type" "text/plain"}
                       :body (str "Oh I love this post: "
                                  "http://localhost:38888/target")})
                    (GET "/target" []
                      {:status 200 
                       :headers {"Content-Type" "text/plain"}
                       :body "Nothing here"}))
           created (promise)
           storage (assoc mock-storage
                          :append 
                          (fn [_] (deliver created true)))
           server (run-jetty handler {:port 38888 :join? false})]
       (p/process-webmention {:s "http://localhost:38888/source"
                              :t "http://localhost:38888/target"}
                             mock-callbacks
                             storage)
       (is (deref created 1000 nil))
       (.stop server))))

(deftest new-webmention-content-type-fail-test
  (testing "One of source and target has unsupported Content-Type"
    (let [handler (routes
                    (GET "/source" []
                      {:status 200 
                       :headers {"Content-Type" "text/plain"}
                       :body (str "Oh I love this post: "
                                  "http://localhost:38888/target")})
                    (GET "/target" []
                      {:status 200 
                       :headers {"Content-Type" "application/json"}
                       :body "Nothing here"}))
           created (promise)
           storage (assoc mock-storage
                          :append 
                          (fn [_] (deliver created true)))
           server (run-jetty handler {:port 38888 :join? false})]
       (p/process-webmention {:s "http://localhost:38888/source"
                              :t "http://localhost:38888/target"}
                             mock-callbacks
                             storage)
       (is (not (deref created 1000 nil)))
       (.stop server))))

(deftest new-webmention-no-target-url-fail-test
  (testing "The source does not contain target URL"
    (let [handler (routes
                    (GET "/source" []
                      {:status 200 
                       :headers {"Content-Type" "text/plain"}
                       :body (str "Oh I love this post "
                                  "but I don't mention it.")})
                    (GET "/target" []
                      {:status 200 
                       :headers {"Content-Type" "application/json"}
                       :body "Nothing here"}))
           created (promise)
           storage (assoc mock-storage
                          :append 
                          (fn [_] (deliver created true)))
           server (run-jetty handler {:port 38888 :join? false})]
       (p/process-webmention {:s "http://localhost:38888/source"
                              :t "http://localhost:38888/target"}
                             mock-callbacks
                             storage)
       (is (not (deref created 1000 nil)))
       (.stop server))))
