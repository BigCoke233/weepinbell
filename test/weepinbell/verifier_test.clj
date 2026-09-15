(ns weepinbell.verifier-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.verifier :as v]
            [weepinbell.test-utility :refer [with-webmention disable-logging]]))

(disable-logging)

(deftest verification-happy-paths-test
  (testing "Test if processor can correctly verifies a Webmention."
    (with-webmention :new ["/source" {:status 200
                                      :headers {"Content-Type" "text/plain; charset=utf-8"}
                                      :body (str "Oh I love this post:"
                                                 " http://localhost:38888/target\n"
                                                 "       this is very clever"
                                                 " and I find it inspiring")}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/html"}}]
      (is (= true (v/verify-mention "http://localhost:38888/source"
                                    "http://localhost:38888/target")))))
  (testing "Test if processor can correctly verifies a Webmention."
    (with-webmention :new ["/source" {:status 206
                                      :headers {"Content-Type" "text/html"}
                                      :body (str "Oh I love this post:"
                                                 " http://localhost:38888/target\n"
                                                 "       this is very clev")}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (= true (v/verify-mention "http://localhost:38888/source"
                                    "http://localhost:38888/target"))))))

(deftest verification-exceptional-cases-test
  (testing "source is not found"
    (with-webmention :new ["/source" {:status 404
                                      :headers {"Content-Type" "text/plain"}
                                      :body "Not Found."}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (not (v/verify-mention "http://localhost:38888/source"
                                 "http://localhost:38888/target")))))
  (testing "source returns error status 502"
    (with-webmention :new ["/source" {:status 502
                                      :headers {"Content-Type" "text/plain"}
                                      :body "Bad Gateway"}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (not (v/verify-mention "http://localhost:38888/source"
                                 "http://localhost:38888/target")))))
  (testing "source body does not contain any URL"
    (with-webmention :new ["/source" {:status 200
                                      :headers {"Content-Type" "text/plain"}
                                      :body "I didn't mention anyone"}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (not (v/verify-mention "http://localhost:38888/source"
                                 "http://localhost:38888/target")))))
  (testing "source body does not contain target URL"
    (with-webmention :new ["/source" {:status 200
                                      :headers {"Content-Type" "text/plain"}
                                      :body (str "I mentioned christ but not alice\n"
                                                 "https://chris.blog/archive/xxx")}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (not (v/verify-mention "http://localhost:38888/source"
                                 "http://localhost:38888/target")))))
  (testing "source response has no Content-Type header"
    (with-webmention :new ["/source" {:status 200
                                      :body (str "I mentioned alice's other post\n"
                                                 "https://alice.blog/article/efg")}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (not (v/verify-mention "http://localhost:38888/source"
                                 "http://localhost:38888/target")))))
  (testing "target response has no Content-Type header"
    (with-webmention :new ["/source" {:status 200
                                      :headers {"Content-Type" "text/plain"}
                                      :body (str "I mentioned alice's other post\n"
                                                 "https://alice.blog/article/efg")}
                           "/target" {:status 200}]
      (is (not (v/verify-mention "http://localhost:38888/source"
                                 "http://localhost:38888/target")))))
  (testing "source response has unsupported Content-Type"
    (with-webmention :new ["/source" {:status 200
                                      :headers {"Content-Type" "image/png"}}
                           "/target" {:status 200}]
      (is (not (v/verify-mention "http://localhost:38888/source"
                                 "http://localhost:38888/target"))))))

(deftest verification-error-test 
  (testing "Host is not found"
    (with-webmention :new ["/source" {}
                           "/target" {:status 200}]
      (is (not (v/verify-mention "http://localh/source" ;; invalid host
                                 "http://localhost:38888/target")))))
  (testing "Host is not found"
    (with-webmention :new ["/source" {}
                           "/target" {:status 200}]
      (is (not (v/verify-mention "http://localh/source" ;; invalid host
                                 "http://localhost:38888/target"))))))

