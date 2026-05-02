(ns weepinbell.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.processor :refer [process-webmention]]
            [weepinbell.handler :refer [app]]
            [weepinbell.channel :refer [wmc]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.webhook :as webhook]
            [taoensso.timbre :as timbre])
  (:gen-class)) ;; Essential for "lein uberjar"


(defn -main [& args]
  ;; Start webmention processor
  (process-webmention wmc
    (let [hooks [:on-receive :on-update :on-delete :on-create]
          webhooks (get-config :webhook)]
      (zipmap hooks 
              (map (fn [hook] 
                     #(do (timbre/info (name hook) "callback triggered.")
                          (webhook/trigger hook webhooks))) 
                   hooks))))
;; Check for a PORT environment variable, default to 3000
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (run-jetty app {:port port :join? true})))

