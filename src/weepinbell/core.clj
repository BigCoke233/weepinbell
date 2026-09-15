(ns weepinbell.core
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.spec.alpha :as s]
            [weepinbell.processor :as processor]
            [weepinbell.handler :refer [app]]
            [weepinbell.config :refer [load-config]]
            [weepinbell.webhook :as webhook]
            [weepinbell.storage.edn-storage :as edn]
            [clojure.core.async :refer [chan]]
            [weepinbell.storage.spec])
  (:gen-class)) ;; Essential for "lein uberjar"

(def wmc (chan))

(def config (load-config))

(def store (s/conform :weepinbell.storage.spec/vtable
                      (edn/use-store (:data-path config))))

(def hooks (let [hooks [:on-receive :on-update :on-delete :on-create]
                 webhooks (:webhook config)]
             (zipmap hooks
                     (map (fn [hook] #(webhook/trigger hook webhooks))
                          hooks))))

(defn -main [& _]
  ;; Start webmention processor
  (processor/start wmc hooks store config)
  ;; Start receiver server
  ;; Check for a PORT environment variable, default to 3000
  (let [port (Integer/parseInt (or (:port config) (System/getenv "PORT") "3000"))]
    (run-server (app wmc store config) {:port port :join? true})))

