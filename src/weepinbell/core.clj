(ns weepinbell.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.processor :as processor]
            [weepinbell.handler :refer [app]]
            [weepinbell.channel :refer [wmc]]
            [weepinbell.config :refer [get-config load-config]]
            [weepinbell.webhook :as webhook]
            [taoensso.timbre :as timbre]
            [weepinbell.storage.edn-storage :as edn])
  (:gen-class)) ;; Essential for "lein uberjar"

(def store (edn/use-store (get-config :data-path)))

(defn -main [& _]
  ;; Start webmention processor
  (processor/start wmc
                   ;; plug in event hooks
                   (let [hooks [:on-receive :on-update :on-delete :on-create]
                         webhooks (get-config :webhook)]
                     (zipmap hooks
                             (map (fn [hook]
                                    #(do (timbre/info (name hook) "callback triggered.")
                                         (webhook/trigger hook webhooks)))
                                  hooks)))
                   ;; plug in storage layer
                   store
                   ;; load configuration
                   (load-config))
  ;; Start receiver server
  ;; Check for a PORT environment variable, default to 3000
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (run-jetty (app store (load-config)) 
               {:port port :join? true})))

