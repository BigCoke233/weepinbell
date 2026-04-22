(ns weepinbell.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.processor :refer [process-webmention]]
            [weepinbell.handler :refer [app wmc]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.webhook :as webhook]
            [taoensso.timbre :as timbre])
  (:gen-class)) ;; Essential for "lein uberjar"


(defn -main [& args]
  ;; Start webmention processor
  (process-webmention wmc 
    {:on-receive (fn [] 
                   (timbre/info "Receive callback:")
                   (webhook/trigger :on-receive (get-config :webhook)))
     :on-update (fn [] 
                  (timbre/info "Update callback:")
                  (webhook/trigger :on-update (get-config :webhook)))
     :on-delete (fn [] 
                  (timbre/info "Delete callback:")
                  (webhook/trigger :on-delete (get-config :webhook)))
     :on-create (fn [] 
                  (timbre/info "Create callback:")
                  (webhook/trigger :on-create (get-config :webhook)))})
  ;; Check for a PORT environment variable, default to 3000
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (run-jetty app {:port port :join? true})))

