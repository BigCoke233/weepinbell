(ns weepinbell.utils-url-test
  (:require [weepinbell.utils.url :as url]
            [clojure.test :refer [deftest testing is]]))

(deftest valid-http-url-test
  (testing "valid url, return true"
    (is (url/valid-http-url? "https://example.com/page/abc?param1=a&param2=b#anchor"))
    (is (url/valid-http-url? "http://www.example.com/")))
  (testing "url not a string, returns false"
    (is (not (url/valid-http-url? 1)))
    (is (not (url/valid-http-url? [])))
    (is (not (url/valid-http-url? {})))
    (is (not (url/valid-http-url? '())))
    (is (not (url/valid-http-url? :not-url)))
    (is (not (url/valid-http-url? true))))
  (testing "malformed url, return false"
    (is (not (url/valid-http-url? "123")))
    (is (not (url/valid-http-url? "example.com")))
    (is (not (url/valid-http-url? "http//example.com")))
    (is (not (url/valid-http-url? "http:example.com"))))
  (testing "not the right scheme, return false"
    (is (not (url/valid-http-url? "ftp://example.com"))))
  (testing "localhost, return false"
    (is (not (url/valid-http-url? "http://localhost")))))

(deftest getter-tests
  (testing "getting domain"
    (is (= "www.example.com" (url/get-domain "https://www.example.com/index.php?page=abc")))
    (is (= "example.com" (url/get-domain "https://example.com/page/abc#anchor"))))
  (testing "getting path"
    (is (= "/index.php" (url/get-path "https://www.example.com/index.php?page=abc")))
    (is (= "/page/abc" (url/get-path "https://example.com/page/abc#anchor"))))
  (testing "getting target"
    (is (= "https://www.example.com" (url/get-target {:webmention {:target "https://www.example.com"}}))))) 

(deftest url-equal-test 
  (testing "URL equality"
    (is (url/url= "https://example.com/./abc" "https://example.com/abc"))
    (is (url/url= "https://example.com/page/../abc" "https://example.com/abc"))
    (is (url/url= "https://example.com/page/abc" "https://example.com/page/abc/"))
    (is (url/url= "https://example.com/page#heading-1" "https://example.com/page/"))
    (is (url/url= "https://example.com/p?b=123&a=321" "https://example.com/p/?a=321&b=123"))
    (is (url/url= "https://example.com/p?b=123&a=321" "https://example.com/p/?a=321&b=123"))))

(defn- has-same-elements? [& colls]
  (apply = (map set colls)))
(deftest url-extract-test 
  (testing "Extracting URLs from string"
    (is (has-same-elements? 
          ["https://test.com" "https://example.com"]
          (url/extract "two links: https://test.com and https://example.com")))
    (is (has-same-elements? 
          ["http://www.example.com/index.php?page=123#anchor"]
          (url/extract "complicated one (http://www.example.com/index.php?page=123#anchor)")))))

(deftest relative-predicate-test
  (testing "Check if a URL is relative"
    (is (url/relative? "./asset/photo.png"))
    (is (url/relative? "../photo.png"))
    (is (url/relative? "/photo.png"))))

(deftest get-base-test
  (testing "Check if get-base works"
    (is (= "https://source.net"
           (url/get-base "https://source.net/page/index.php?param=value#test")))
    (is (not= "https://source.net/"
              (url/get-base "https://source.net/page/index.php?param=value#test")))))

