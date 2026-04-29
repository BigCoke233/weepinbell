(ns weepinbell.utils-test
  (:require [weepinbell.utils :as utils]
            [clojure.test :refer [deftest testing is]]))

((deftest get-domain-test
      (testing "Testing utils/get-domain"
        (is (= (utils/get-domain "https://example.com/era23#123")
               "example.com"))
        (is (= (utils/get-domain "http://sub.example.com/era23?quer#123")
               "sub.example.com"))
        (is (= (utils/get-domain "https://eada.xasdw/wada")
               "eada.xasdw")))))

((deftest get-path-test
      (testing "Testing utils/get-path"
        (is (= (utils/get-path "https://neodb.social/tv/season/1m09Jn4wPIyzC2m2J1ERGE")
               "/tv/season/1m09Jn4wPIyzC2m2J1ERGE"))
        (is (= (utils/get-path "https://eltr.ac/")
               "/"))
        (is (= (utils/get-path "https://www.example.com/path/some.html?flag=true")
               "/path/some.html")))))

((deftest valid-http-url-test
      (testing "Testing valid-http-url?"
        (is (= (utils/valid-http-url? "ssh://example.com/http/")
               false))
        (is (= (utils/valid-http-url? "shttp://example.com/")
               false))
        (is (= (utils/valid-http-url? "http//example.com")
               false))
        (is (= (utils/valid-http-url? "example.com/path/to/post")
               false))
        (is (= (utils/valid-http-url? "https://www.example")
               false))
        (is (= (utils/valid-http-url? "http://localhost/path")
               false))
        (is (= (utils/valid-http-url? "https://www.example.com/path?query=string#anchor")
               true))
        (is (= (utils/valid-http-url? "http://simple.top")
               true)))) )

((deftest format-list-test
      (testing "Testing utils.format-list"
        (is (= (utils/format-list ["abc", "213", "$#@"])
               "abc, 213 and $#@"))
        (is (= (utils/format-list ["abc"])
               "abc"))
        (is (= (utils/format-list ["123", 456])
               "123 and 456"))
        (is (= (utils/format-list [])
               "")))) )

