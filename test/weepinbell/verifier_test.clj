(ns weepinbell.verifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [compojure.core :refer [GET routes]]
            [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.verifier :as v]
            [weepinbell.test-utility :refer [with-webmention]]))

(deftest verification-happy-paths-test
  (testing "Test if processor can correctly verifies a Webmention."
    (with-webmention :new ["/source" {:status 200
                                      :headers {"Content-Type" "text/plain"}
                                      :body (str "Oh I love this post:"
                                                 " http://localhost:38888/target\n"
                                                 "       this is very clever"
                                                 " and I find it inspiring")}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (= true (v/verify-mention "http://localhost:38888/source"
                                    "http://localhost:38888/target")))))
  (testing "Test if processor can correctly verifies a Webmention."
    (with-webmention :new ["/source" {:status 206
                                      :headers {"Content-Type" "text/plain"}
                                      :body (str "Oh I love this post:"
                                                 " http://localhost:38888/target\n"
                                                 "       this is very clev")}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (= true (v/verify-mention "http://localhost:38888/source"
                                    "http://localhost:38888/target"))))))

(deftest verification-error-cases-test
  (testing "Test if processor can correctly verifies a Webmention."
    (let [handler (routes
                   ;; Cases that source respond with an error
                   (GET "/a" []
                     {:status 404
                      :headers {"Content-Type" "text/plain"}
                      :body "Not Found."})
                   (GET "/b" []
                     {:status 502
                      :headers {"Content-Type" "text/plain"}
                      :body "Bad Gateway"})
                   ;; Cases that source does not contain target URL
                   (GET "/c" []
                     {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body "I didn't mention anyone"})
                   (GET "/d" []
                     {:status 200
                      :headers {"Content-Type" "text/plain"}
                      :body (str "I mentioned christ but not alice\n"
                                 "https://chris.blog/archive/xxx")})
                   (GET "/e" []
                     {:status 200
                      :body (str "I mentioned alice's other post\n"
                                 "https://alice.blog/article/efg")}))
          server (run-jetty handler {:port 28888 :join? false})]
      ;; Cases that source respond with an error
      (->> ["a" "b" "c" "d" "e" "f"]
           (map #(is (not
                      (v/verify-mention (str "http://localhost:28888/" %)
                                        "https://alice.blog/article/abc"))))
           doall)
      (.stop server))))

