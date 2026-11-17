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
            [weepinbell.webserver.receiver :refer [receive receive-batch]]))

(defn- get-error-message [error-type]
  (error-type {:invalid-url "Invalid URLs. Source and target have to be valid http(s) urls."
        :identical "Source and target are identical."
        :domain-not-allowed "Domain of target is not allowed. This receiver does not receive webmention on its behalf."}))

(def happy-paths [["https://bob.blog" "https://alice.blog"]
                  ["http://bob.blog" "http://alice.blog"]
                  ["https://bob.blog/post/123" "https://alice.blog/article/abc"]
                  ["http://bob.blog/post/123" "http://alice.blog/article/abc"]])
(def error-cases [;; invalid URLs
                  ["htt://bob.blog" "https://alice.blog/article/abc" :invalid-url]
                  ["ftp://bob.blog/file/x" "https://alice.blog/article/abc" :invalid-url]
                  ["bob.blog/post/123" "https://alice.blog/article/abc" :invalid-url]
                  ["https://bob.blog/post/123" "alice.blog/article/abc" :invalid-url]
                  ["https://bob.blog/post/123" "ftp://alice.blog/file/x" :invalid-url]
                  ;; identical URLs 
                  ["https://alice.blog/article/abc" "https://alice.blog/article/abc" :identical]
                  ["https://alice.blog/article/abc/" "https://alice.blog/article/abc" :identical]
                  ["https://alice.blog/article/abc#anchor" "https://alice.blog/article/abc" :identical]
                  ["https://alice.blog/article/abc?a=1&b=2" "https://alice.blog/article/abc?b=2&a=1" :identical]
                  ;; domain not allowed 
                  ["https://bob.blog/post/123" "https://chris.blog/archive/xxx" :domain-not-allowed]
                  ;; nil input 
                  [nil nil :invalid-url]])

;; webmention queue for testing
(def test-wmc (chan))
(def test-config {:allowed-target-domains ["alice.blog"]
                  :batch-receive-limit 100})

;; validation tests
(deftest happy-path-test
  (testing "Receiver happy paths testing."
    (doseq [path happy-paths]
      (let [[s t] path
            response (receive {:s s :t t}
                              test-wmc
                              test-config)]
        (is (= 201 (:status response)))))))

(deftest error-response-test
  (testing "Receiver error handling."
    (doseq [path error-cases]
      (let [[s t error-type] path
            response (receive {:s s :t t}
                              test-wmc
                              test-config)]
        (is (= 400 (:status response)))
        (is (= (get-error-message error-type)
               (get-in response [:body :message])))))))
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
                  (receive wm test-queue test-config)
                  (is (= wm (poll! test-queue))))))
         doall)
     ;; invalid webmention 
    (->> error-cases
         (map (fn [testcase]
                (let [[s t] testcase
                      wm {:s s :t t}
                      test-queue (chan)]
                  (receive wm test-queue test-config)
                  (is (not= wm (poll! test-queue))))))
         doall)))
;; end in-queue test

;; batch receive test
(def successful-requests
  (map (fn [[s t]] {:s s :t t}) happy-paths))
(def unsuccessful-requests
  (map (fn [[s t]] {:s s :t t}) error-cases))

(deftest batch-receive-test
  (testing "Putting multiple successful Webmention requests at once."
    (let [resp (receive-batch successful-requests test-wmc test-config)]
      (is (= (:status resp) 200))
      (is (= (count (get-in resp [:body :accepted]))
             (count successful-requests)))
      (is (= 0 (count (get-in resp [:body :rejected]))))))
  (testing "Putting multiple unsuccessful Webmention requests at once."
    (let [resp (receive-batch unsuccessful-requests test-wmc test-config)]
      (is (= (:status resp) 400))
      (is (= (count (get-in resp [:body :rejected]))
             (count unsuccessful-requests)))
      (is (= 0 (count (get-in resp [:body :accepted]))))))
  (testing "Putting mixed Webmention requests at once."
    (let [mixed-requests (reduce conj successful-requests unsuccessful-requests)
          resp (receive-batch mixed-requests test-wmc test-config)]
      (is (= (:status resp) 202))
      (is (= (get-in resp [:body :message]) "Some webmentions are received. Some are invalid."))
      (is (= (count (get-in resp [:body :rejected]))
             (count unsuccessful-requests)))
      (is (= (count (get-in resp [:body :accepted]))
             (count successful-requests)))))
  (testing "Putting too many requests at once should reject those exceeding limits"
    (let [limit 100
          mixed-requests (reduce conj successful-requests unsuccessful-requests)
          too-many (reduce into [] (repeat limit mixed-requests))
          resp (receive-batch too-many test-wmc test-config)
          accepted (get-in resp [:body :accepted])
          rejected (get-in resp [:body :rejected])]
      (is (= (:status resp) 202))
      (is (= (get-in resp [:body :message])
             (str "Too many webmentions. Some were dropped. " 
                  "Only first " limit " were accepted.")))
      (is (>= limit (+ (count accepted) (count rejected)))))))

