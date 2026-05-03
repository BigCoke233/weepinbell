;; The receiver only verifies URLs, and does not make HTTP requests 
;; Verifying Content-Type and if target url is present in source 
;; is the processor's concern. Thus this test does not contain any 
;; test cases that covers those checks.

;; Receiver does this things: 
;; 1. Receive a webmention request from handler 
;; 2. Validate request 
;;   2.1 Source and target should be valid URLs;
;;   2.2 Source and target should not be identical; 
;;   2.3 Domain of target is in allow list. 
;; 3. Put validated request into queue.

(ns weepinbell.receiver-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.core.async :refer [chan]]
            [weepinbell.receiver :refer [receive]]))

;; webmention queue for testing
(def test-wmc (chan))

(defn- wrap-400-msg [typ]
  (let [msg (typ {:invalid-url "Invalid URLs. Source and target have to be valid http(s) urls."
                  :identical "Source and target are identical."
                  :domain-not-allowed "Domain of target is not allowed. This receiver does not receive webmention on its behalf."})]
    {:status 400
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body {:status 400
     :message msg}}))

((deftest happy-path-test
    (testing "Receiver happy paths testing."
        (->> [["https://bob.blog" "https://alice.blog"]
              ["http://bob.blog" "http://alice.blog"]
              ["https://bob.blog/post/123" "https://alice.blog/article/abc"]
              ["http://bob.blog/post/123" "http://alice.blog/article/abc"]]
        (map (fn [testcase]
               (let [[s t] testcase]
                 (is (= 201
                        (:status (receive {:s s :t t}
                                  test-wmc
                                  {:domains ["alice.blog"]})))))))
        doall))))
((deftest validation-test
    (testing "Receiver error handling."
        (->> [;; invalid URLs
              ["htt://bob.blog" "https://alice.blog/article/abc" (wrap-400-msg :invalid-url)]
              ["ftp://bob.blog/file/x" "https://alice.blog/article/abc" (wrap-400-msg :invalid-url)]
              ["bob.blog/post/123" "https://alice.blog/article/abc" (wrap-400-msg :invalid-url)]
              ["https://bob.blog/post/123" "alice.blog/article/abc" (wrap-400-msg :invalid-url)]
              ["https://bob.blog/post/123" "ftp://alice.blog/file/x" (wrap-400-msg :invalid-url)]
              ;; identical URLs 
              ["https://alice.blog/article/abc" "https://alice.blog/article/abc" (wrap-400-msg :identical)]
              ;; domain not allowed 
              ["https://bob.blog/post/123" "https://chris.blog/archive/xxx" (wrap-400-msg :domain-not-allowed)]]
        (map (fn [testcase]
               (let [[s t expects] testcase]
                 (is (= expects 
                        (receive {:s s :t t}
                          test-wmc
                          {:domains ["alice.blog"]}))))))
        doall))))

