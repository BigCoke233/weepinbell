(ns weepinbell.processor-test
  (:require [clojure.test :refer [deftest testing is]]
            [compojure.core :refer [GET routes]]
            [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.processor :as p]))

(deftest verification-happy-paths-test
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

(deftest verification-error-cases-test
  (testing "Test if processor can correctly verifies a Webmention."
    (let [handler (routes
                   ;; Cases that source respond with an error
                   (GET "/a" []
                     {:status 404 :headers {}
                      :body "Not Found."})
                   (GET "/b" []
                     {:status 502 :headers {}
                      :body "Bad Gateway"})
                   ;; Cases that source does not contain target URL
                   (GET "/c" []
                     {:status 200 :headers {}
                      :body "I didn't mention anyone"})
                   (GET "/d" []
                     {:status 200 :headers {}
                      :body (str "I mentioned christ but not alice\n"
                                 "https://chris.blog/archive/xxx")})
                   (GET "/e" []
                     {:status 200 :headers {}
                      :body (str "I mentioned alice's other post\n"
                                 "https://alice.blog/article/efg")}))
          server (run-jetty handler {:port 28888 :join? false})]
      ;; Cases that source respond with an error
      (->> ["a" "b" "c" "d" "e" "f"]
           (map #(is (not
                      (p/verify-mention (str "http://localhost:28888/" %)
                                        "https://alice.blog/article/abc"))))
           doall)
      (.stop server))))

