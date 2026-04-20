(ns weepinbell.handler-test
  (:require [clojure.test :refer [testing is deftest]]
            [ring.mock.request :as mock]
            [weepinbell.handler :refer [app]]))

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

