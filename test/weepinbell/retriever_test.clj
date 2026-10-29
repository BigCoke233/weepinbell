(ns weepinbell.retriever-test
  (:require [clojure.test :refer [deftest testing is]]
            [weepinbell.utils.digest :refer [digest]]
            [weepinbell.webserver.retriever :as ret]))

(def page-size 1)

(defn- make-bare-wm [number]
  {:webmention {:source (str number) :target (str number)
                :id (digest (str number)) 
                :modified (System/currentTimeMillis)}})
(defn- make-typed-wm [type number]
  (-> (make-bare-wm number)
      (assoc :jf2 {:wm-property type})))

(deftest retrieve-by-domain-test
  (testing "by-domain list paging"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-data (mapv make-bare-wm (range 2))
            mock-store {:get-by-domain (fn [& _] mock-data)}
            resp0 (ret/by-domain "" 0 mock-store {})
            resp1 (ret/by-domain "" 1 mock-store {})]
        (is (= page-size (count (:body resp0))))
        (is (= page-size (count (:body resp1)))))))
  (testing "by-domain return 404 when list is empty"
    (let [mock-store {:get-by-domain (fn [& _] [])}
          resp (ret/by-domain "" nil mock-store {})]
      (is (= 404 (:status resp)))
      (is (= "The requested webmention list does not exist."
             (get-in resp [:body :message]))))))

(deftest retrieve-by-url-test
  (testing "by-url list paging"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-data (mapv make-bare-wm (range 2))
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
            resp (ret/by-url "" nil mock-store {})]
        (is (= 404 (:status resp)))
        (is (= "The requested webmention list does not exist."
               (get-in resp [:body :message])))))))

(defn- should-404 [data]
  (let [mock-store {:get (fn [& _] data)}
          resp (ret/by-id "" mock-store)]
      (is (= 404 (:status resp)))
      (is (= "The requested webmention does not exist."
             (get-in resp [:body :message])))))
(deftest retrieve-by-id-test
  (testing "by-id return 404 when no webmention is matched"
    (doseq [no-data [{} [] nil]]
      (should-404 no-data))))

(deftest filter-by-jf2type-test
  (testing "by-domain ?jf2type filter"
    (let [like-wms (mapv (partial make-typed-wm :like-of) (range 2))
          reply-wms (mapv (partial make-typed-wm :in-reply-to) (range 3))
          mock-data (reduce into [] [like-wms reply-wms])
          mock-store {:get-by-domain (fn [& _] mock-data)}
          ;; responses
          resp-like (ret/by-domain "" 0 mock-store {:type :like-of})
          resp-reply (ret/by-domain "" 0 mock-store {:type :in-reply-to})]
      (is (= reply-wms (:body resp-reply)))
      (is (= like-wms (:body resp-like)))))
  (testing "by-url ?jf2type filter"
    (let [like-wms (mapv (partial make-typed-wm :like-of) (range 2))
          reply-wms (mapv (partial make-typed-wm :in-reply-to) (range 3))
          mock-data (reduce into [] [like-wms reply-wms])
          mock-store {:get-by-domain (fn [& _] mock-data)}
          ;; responses
          resp-like (ret/by-url "" 0 mock-store {:type :like-of})
          resp-reply (ret/by-url "" 0 mock-store {:type :in-reply-to})]
      (is (= reply-wms (:body resp-reply)))
      (is (= like-wms (:body resp-like))))))

(deftest paging-edge-cases-test
  (testing "if page is a string that can be parsed to number, it should work"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-data (mapv make-bare-wm (range 2))
            mock-store {:get-by-domain (fn [& _] mock-data)}
            resp0 (ret/by-domain "" "0" mock-store {})
            resp1 (ret/by-domain "" 0 mock-store {})]
        (is (= resp0 resp1)))))
  (testing "if page is not number, return the last page frame"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-data (mapv make-bare-wm (range 2))
            mock-store {:get-by-domain (fn [& _] mock-data)}
            resp (ret/by-domain "" true mock-store {})]
        (is (= 200 (:status resp)))
        (is (= (-> mock-data last :webmention)
               (-> resp :body last :webmention))))))
  (testing "if page is larger than total amount of pages, return error"
    (with-redefs [weepinbell.webserver.retriever/default-page-size page-size]
      (let [mock-data [(make-bare-wm page-size)]
            mock-store {:get-by-domain (fn [& _] mock-data)}
            resp (ret/by-domain "" (inc page-size) mock-store {})]
        (is (= 400 (:status resp)))))))
