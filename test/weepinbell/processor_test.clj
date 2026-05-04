(ns weepinbell.processor-test
  (:require [clojure.test :refer [deftest testing is]]
            [compojure.core :refer [GET defroutes]]
            [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.processor :as p]))

;; Test cases
(def happy-paths
  [{:source-path "/1"
    :source-status 200
    :source-content
    "Oh I love this post: https://alice.blog/article/abc
       this is very clever and I find it inspiring"
    :target "https://alice.blog/article/abc"
    :expect true}
   {:source-path "/2"
    :source-status 206
    :source-content
    "Oh I love this post: https://alice.blog/article/abc
      this is very clever and I fi"
    :target "https://alice.blog/article/abc"
    :expect true}])
;; End test cases

;; Conver test cases to routes
;; so test server can serve them
(defmacro make-test-routes [testcases]
  (doall (map
   (fn [testcase]
     `(GET ~(:source-path testcase)
       [] {:status ~(:source-status testcase)
           :headers {}
           :body ~(:source-content testcase)}))
   @(resolve testcases))))

(defroutes test-routes 
  (make-test-routes happy-paths))
;; end defroutes

((deftest verification-test
   (testing "Test if processor can correctly verifies a Webmention."
     (let [server (run-jetty test-routes {:port 28888 :join? false})]
       (->> happy-paths
            (map #(is (= (:expect %)
                         (p/verify-mention (str "http://localhost:28888"
                                                (:source-path %))
                                           (:target %)))))
            doall)
       (.stop server)))))
