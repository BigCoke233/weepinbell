(ns weepinbell.test-utility
  (:require [compojure.core :refer [GET routes]]
            [org.httpkit.server :refer [run-server]]
            [weepinbell.processor :as p]))

(def mock-callbacks {:on-create  (fn [])
                     :on-delete  (fn [])
                     :on-update  (fn [])
                     :on-receive (fn [])})

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
                      :get     (fn [~'_ ~'_] (= ~existance :old))}
           ~'server (run-server handler# {:port 38888 :join? false})]
       (p/process-webmention {:s ~source-url :t ~target-url}
                             mock-callbacks
                             ~'storage
                             {})
       ~@body
       (~'server))))

