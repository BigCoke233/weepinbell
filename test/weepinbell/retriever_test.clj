(ns weepinbell.retriever-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.webserver.retriever :as ret]))

(def page-size 1)
(deftest retrieve-by-domain-test
  (testing "by-domain list paging"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-data [{:webmention {:source "1" :target "1"}}
                       {:webmention {:source "2" :target "2"}}]
            mock-store {:get-by-domain (fn [& _] mock-data)}
            resp0 (ret/by-domain "" 0 mock-store {})
            resp1 (ret/by-domain "" 1 mock-store {})]
        (is (= page-size (count (:body resp0))))
        (is (= page-size (count (:body resp1)))))))
  (testing "by-domain: if page is not number, return the last page frame"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-data [{:webmention {:source "1" :target "1"}}
                       {:webmention {:source "2" :target "2"}}]
            mock-store {:get-by-domain (fn [& _] mock-data)}
            resp (ret/by-domain "" true mock-store {})]
        (is (= 200 (:status resp)))
        (is (= (-> mock-data last :webmention)
               (-> resp :body last :webmention))))))
  (testing "by-domain return 404 when list is empty"
    (let [mock-store {:get-by-domain (fn [& _] [])}
          resp (ret/by-domain "" nil mock-store {})]
      (is (= 404 (:status resp)))
      (is (= "The requested webmention list does not exist."
             (get-in resp [:body :message]))))))

(deftest retrieve-by-url-test
  (testing "by-url list paging"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-data [{:webmention {:source "1" :target "2"}}
                       {:webmention {:source "1" :target "2"}}]
            mock-store {:get-by-domain (fn [& _] mock-data)}
            resp0 (ret/by-url "2" 0 mock-store {})
            resp1 (ret/by-url "2" 1 mock-store {})]
        (is (= page-size (count (:body resp0))))
        (is (= page-size (count (:body resp1)))))))
  (testing "by-url with no url provieded"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-store {:get-by-domain (fn [& _])}
            resp (ret/by-url nil 0 mock-store {})]
        (is (= 400 (:status resp)))
        (is (= "Missing URL. Specify target URL whose webmentions you are querying."
               (get-in resp [:body :message]))))))
  (testing "by-url return 404 when list is empty"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-store {:get-by-domain (fn [& _])}
            resp (ret/by-url "" 0 mock-store {})]
        (is (= 404 (:status resp)))
        (is (= "The requested webmention list does not exist."
               (get-in resp [:body :message])))))))

(deftest retrieve-by-id-test
  (testing "by-id return 404 when no webmention is matched"
    (let [mock-store {:get (fn [& _] [])}
          resp (ret/by-id "" mock-store {})]
      (is (= 404 (:status resp)))
      (is (= "The requested webmention does not exist."
             (get-in resp [:body :message]))))))
