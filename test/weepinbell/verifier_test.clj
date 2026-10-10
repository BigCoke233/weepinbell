(ns weepinbell.verifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.processor.verifier :as v]
            [weepinbell.http.client]
            [weepinbell.test-utility :refer [with-webmention]]))

(defn- test-verify [source target]
  (-> {:wm {:s source :t target}
       :ok? true
       :id "123"}
      v/verify
      :verified?))

(deftest verification-happy-paths-test
  (testing "Test if processor can correctly verifies a Webmention."
    (with-webmention :new ["/source" {:status 200
                                      :headers {"Content-Type" "text/plain; charset=utf-8"}
                                      :body (str "Oh I love this post:"
                                                 " http://localhost:38888/target\n"
                                                 "       this is very clever"
                                                 " and I find it inspiring")}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (= true (test-verify "http://localhost:38888/source"
                               "http://localhost:38888/target")))))
  (testing "Test if processor can correctly verifies a Webmention."
    (with-webmention :new ["/source" {:status 206
                                      :headers {"Content-Type" "text/html"}
                                      :body (str "Oh I love this post:"
                                                 " http://localhost:38888/target\n"
                                                 "       this is very clev")}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (= true (test-verify "http://localhost:38888/source"
                               "http://localhost:38888/target"))))))

(deftest verification-exceptional-cases-test
  (with-redefs [weepinbell.http.client/default-options {:timeout 100}]
    (testing "source is not found"
      (with-webmention :new ["/source" {:status 404
                                        :headers {"Content-Type" "text/plain"}
                                        :body "Not Found."}
                             "/target" {:status 200
                                        :headers {"Content-Type" "text/plain"}}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))
    (testing "source returns error status 502"
      (with-webmention :new ["/source" {:status 502
                                        :headers {"Content-Type" "text/plain"}
                                        :body "Bad Gateway"}
                             "/target" {:status 200
                                        :headers {"Content-Type" "text/plain"}}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))
    (testing "source body does not contain any URL"
      (with-webmention :new ["/source" {:status 200
                                        :headers {"Content-Type" "text/plain"}
                                        :body "I didn't mention anyone"}
                             "/target" {:status 200
                                        :headers {"Content-Type" "text/plain"}}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))
    (testing "source body does not contain target URL"
      (with-webmention :new ["/source" {:status 200
                                        :headers {"Content-Type" "text/plain"}
                                        :body (str "I mentioned christ but not alice\n"
                                                   "https://chris.blog/archive/xxx")}
                             "/target" {:status 200
                                        :headers {"Content-Type" "text/plain"}}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))
    (testing "source response has no Content-Type header"
      (with-webmention :new ["/source" {:status 200
                                        :body (str "I mentioned alice's other post\n"
                                                   "https://alice.blog/article/efg")}
                             "/target" {:status 200
                                        :headers {"Content-Type" "text/plain"}}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))
    (testing "target response has no Content-Type header"
      (with-webmention :new ["/source" {:status 200
                                        :headers {"Content-Type" "text/plain"}
                                        :body (str "I mentioned alice's other post\n"
                                                   "https://alice.blog/article/efg")}
                             "/target" {:status 200}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))
    (testing "unsupported content type"
      (with-webmention :new ["/source" {:status 200
                                        :headers {"Content-Type" "application/json"}
                                        :body "{\"message\": \"I mentioned alice's other post\n https://alice.blog/article/efg\"}"}
                             "/target" {:status 200
                                        :headers {"Content-Type" "text/plain"}}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))
    (testing "responses have no status"
      (with-webmention :new ["/source" {:status nil
                                        :headers {"Content-Type" "text/plain"}
                                        :body (str "I mentioned alice's other post\n"
                                                   "https://alice.blog/article/efg")}
                             "/target" {:headers {"Content-Type" "text/plain"}}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))
    (testing "HTTP not complete, 100 status"
      (with-webmention :new ["/source" {:status 100
                                        :headers {"Content-Type" "text/plain"}
                                        :body "oops"}
                             "/target" {:status 200
                                        :headers {"Content-Type" "text/plain"}}]
        (is (not (test-verify "http://localhost:38888/source"
                              "http://localhost:38888/target")))))))

