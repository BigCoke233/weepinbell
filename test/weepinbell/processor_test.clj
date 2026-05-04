(ns weepinbell.processor-test
  (:require [clojure.test :refer [deftest testing is]]
            [compojure.core :refer [GET routes]]
            [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.processor :as p]))

(deftest verification-test
  (testing "Test if processor can correctly verifies a Webmention."
    (let [handler (routes
                   (GET "/1" []
                     {:status 200 :headers {}
                      :body (str "Oh I love this post:"
                                 " https://alice.blog/article/abc\n"
                                 "       this is very clever"
                                 " and I find it inspiring")})
                   (GET "/2" []
                     {:status 206 :headers {}
                      :body (str "Oh I love this post:"
                                 " https://alice.blog/article/abc\n"
                                 "      this is very clever and I fi")}))
          server (run-jetty handler {:port 28888 :join? false})]
      (is (= true (p/verify-mention "http://localhost:28888/1"
                                    "https://alice.blog/article/abc")))
      (is (= true (p/verify-mention "http://localhost:28888/2"
                                    "https://alice.blog/article/abc")))
      (.stop server))))
