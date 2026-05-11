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
            [clojure.core.async :refer [chan poll!]]
            [weepinbell.receiver :refer [receive]]))

;; utilities
(defn- wrap-400-msg [typ]
  (let [msg (typ {:invalid-url "Invalid URLs. Source and target have to be valid http(s) urls."
                  :identical "Source and target are identical."
                  :domain-not-allowed "Domain of target is not allowed. This receiver does not receive webmention on its behalf."})]
    {:status 400
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body {:status 400
            :message msg}}))
;; end utilities

;; define test cases 
(def happy-paths [["https://bob.blog" "https://alice.blog"]
                  ["http://bob.blog" "http://alice.blog"]
                  ["https://bob.blog/post/123" "https://alice.blog/article/abc"]
                  ["http://bob.blog/post/123" "http://alice.blog/article/abc"]])
(def error-cases [;; invalid URLs
                  ["htt://bob.blog" "https://alice.blog/article/abc" (wrap-400-msg :invalid-url)]
                  ["ftp://bob.blog/file/x" "https://alice.blog/article/abc" (wrap-400-msg :invalid-url)]
                  ["bob.blog/post/123" "https://alice.blog/article/abc" (wrap-400-msg :invalid-url)]
                  ["https://bob.blog/post/123" "alice.blog/article/abc" (wrap-400-msg :invalid-url)]
                  ["https://bob.blog/post/123" "ftp://alice.blog/file/x" (wrap-400-msg :invalid-url)]
                  ;; identical URLs 
                  ["https://alice.blog/article/abc" "https://alice.blog/article/abc" (wrap-400-msg :identical)]
                  ;; domain not allowed 
                  ["https://bob.blog/post/123" "https://chris.blog/archive/xxx" (wrap-400-msg :domain-not-allowed)]])
;; end define test cases

;; webmention queue for testing
(def test-wmc (chan))
;; end webmention queue for testing

;; validation tests
(deftest happy-path-test
  (testing "Receiver happy paths testing."
    (->> happy-paths
         (map (fn [testcase]
                (let [[s t] testcase]
                  (is (= 201
                         (:status (receive {:s s :t t}
                                           test-wmc
                                           {:domains ["alice.blog"]})))))))
         doall)))

(deftest error-response-test
  (testing "Receiver error handling."
    (->> error-cases
         (map (fn [testcase]
                (let [[s t expects] testcase]
                  (is (= expects
                         (receive {:s s :t t}
                                  test-wmc
                                  {:domains ["alice.blog"]}))))))
         doall)))
;; end validation tests

;; in-queue test 
;; see if receiver is able to put webmention request 
;; into queue for processor to pick up
;; and if it falsely put unvalidated request
(deftest in-queue-test
  (testing "Receiver putting Webmention in queue."
     ;; valid webmention
    (->> happy-paths
         (map (fn [testcase]
                (let [[s t] testcase
                      wm {:s s :t t}
                      test-queue (chan)]
                  (receive wm test-queue {:domains ["alice.blog"]})
                  (is (= wm (poll! test-queue))))))
         doall)
     ;; invalid webmention 
    (->> error-cases
         (map (fn [testcase]
                (let [[s t] testcase
                      wm {:s s :t t}
                      test-queue (chan)]
                  (receive wm test-queue {:domains ["alice.blog"]})
                  (is (not= wm (poll! test-queue))))))
         doall)))
;; end in-queue test
