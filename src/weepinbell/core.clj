(ns weepinbell.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.processor :as processor]
            [weepinbell.handler :refer [app]]
            [weepinbell.channel :refer [wmc]]
            [weepinbell.config :refer [get-config load-config]]
            [weepinbell.webhook :as webhook]
            [taoensso.timbre :as timbre]
            [weepinbell.storage :as storage])
  (:gen-class)) ;; Essential for "lein uberjar"

(def store {:append storage/append-wm
                    :update storage/update-wm
                    :delete storage/delete-wm
                    :get    storage/get-wm
                    :get-by-domain storage/get-wms-by-domain})

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
                   store)
  ;; Start receiver server
  ;; Check for a PORT environment variable, default to 3000
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (run-jetty (app store (load-config)) 
               {:port port :join? true})))

