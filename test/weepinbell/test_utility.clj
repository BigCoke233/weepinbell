(ns weepinbell.test-utility
  (:require [compojure.core :refer [GET routes]]
            [org.httpkit.server :refer [run-server]]
            [weepinbell.processor :as p]
            [taoensso.timbre :refer [merge-config!]]))

(def mock-callbacks {:on-create  (fn [_])
                     :on-delete  (fn [_])
                     :on-update  (fn [_])
                     :on-receive (fn [_])})

(defmacro with-webmention [existance [source-route source-resp target-route target-resp] & body]
  (let [source-url (str "http://localhost:38888" source-route)
        target-url (str "http://localhost:38888" target-route)]
    `(let [handler# (routes
                     (GET ~source-route [] ~source-resp)
                     (GET ~target-route [] ~target-resp))
           ~'created (promise)
           ~'deleted (promise)
           ~'updated (promise)
           ~'storage {:append  (fn [~'_] (deliver ~'created true))
                      :update  (fn [~'_ ~'_ ~'_] (deliver ~'updated true))
                      :delete  (fn [~'_ ~'_] (deliver ~'deleted true))
                      :get     (fn [~'_ ~'_]
                                 (when (= ~existance :old)
                                   {:webmention {:id "test-id"
                                                 :source ~source-url
                                                 :target ~target-url}}))}
           ~'server (run-server handler# {:port 38888 :join? false})]
       (try
         (p/process-webmention {:s ~source-url :t ~target-url}
                               mock-callbacks
                               ~'storage
                               {})
         ~@body
         (finally (~'server))))))

(defn disable-logging []
  (merge-config! {:min-level :report}))

