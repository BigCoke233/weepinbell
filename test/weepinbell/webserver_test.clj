(ns weepinbell.webserver-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.core.async :refer [chan]]
            [weepinbell.webserver.facade :refer [start-webserver]]
            [weepinbell.http.client :as client]))

(deftest webserver-start-test
  (testing "Webserver should start and perform a graceful shutdown normally"
    (let [port "48080"
          server-meta {:wmc (chan)
                       :store {:append  (fn [_])
                               :update  (fn [_])
                               :delete  (fn [_])
                               :get     (fn [_])}
                       :config {:port port}}
          stop (start-webserver server-meta)]
      (is (not (:error (client/get! (str "http://localhost:" port)))))
      (stop)
      (is (:error (client/get! (str "http://localhost:" port)))))))
