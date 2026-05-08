(ns weepinbell.processor-test
  (:require [clojure.test :refer [deftest is testing]]
            [weepinbell.test-utility :refer [with-webmention]]))

;; Testing process-new-webmention
;; though we're calling a proxy function (process-webmention)
;; But the arguments and storage vtables that test cases provide
;; should result in a call to process-new-webmention

(deftest new-webmention-test
  (testing "Creating a new webmention."
    (with-webmention :new ["/source" {:status 200
                                 :headers {"Content-Type" "text/plain"}
                                 :body (str "Oh I love this post: "
                                            "http://localhost:38888/target")}
                      "/target" {:status 200
                                 :headers {"Content-Type" "text/plain"}
                                 :body "Nothing here"}]
      (is (deref created 100 nil)))))

(deftest new-webmention-content-type-fail-test
  (testing "One of source and target has unsupported Content-Type"
    (with-webmention :new ["/source" {:status 200
                                 :headers {"Content-Type" "text/plain"}
                                 :body (str "Oh I love this post: "
                                            "http://localhost:38888/target")}
                      "/target" {:status 200
                                 :headers {"Content-Type" "application/json"}
                                 :body "Nothing here"}]
      (is (not (deref created 100 nil))))))

(deftest new-webmention-no-target-url-fail-test
  (testing "The source does not contain target URL"
    (with-webmention :new ["/source" {:status 200
                                 :headers {"Content-Type" "text/plain"}
                                 :body (str "Oh I love this post "
                                            "but I don't mention it.")}
                      "/target" {:status 200
                                 :headers {"Content-Type" "application/json"}
                                 :body "Nothing here"}]
      (is (not (deref created 100 nil))))))

;; Testing process-existing-webmention

(deftest old-webmention-update-test 
  (testing "Updating exisiting webmention if links are still valid."
    (with-webmention :old ["/source" {:status 200 
                                      :headers {"Content-Type" "text/plain"}
                                      :body (str "Oh I love this post "
                                                 "http://localhost:38888/target")}
                           "/target" {:status 200 
                                      :headers {"Content-Type" "text/plain"}
                                      :body "Nothing here"}]
      (is (deref updated 100 nil)))))

(deftest old-webmention-no-target-link-delete-test 
  (testing "Deleting exisiting webmention if source doest no mention target anymore."
    (with-webmention :old ["/source" {:status 200 
                                      :headers {"Content-Type" "text/plain"}
                                      :body "Nothing Here"}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (deref deleted 100 nil)))))
0
(deftest old-webmention-source-not-found-delete-test 
  (testing "Deleting exisiting webmention if source is not there."
    (with-webmention :old ["/source" {:status 404
                                      :headers {"Content-Type" "text/plain"}}
                           "/target" {:status 200
                                      :headers {"Content-Type" "text/plain"}}]
      (is (deref deleted 100 nil)))))

(deftest old-webmention-target-not-found-delete-test
  (testing "Deleting exisiting webmention if source is not there."
    (with-webmention :old ["/source" {:status 200
                                      :headers {"Content-Type" "text/plain"}}
                           "/target" {:status 404
                                      :headers {"Content-Type" "text/plain"}}]
      (is (deref deleted 100 nil)))))

