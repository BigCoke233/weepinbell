(ns weepinbell.core
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.spec.alpha :as s]
            [weepinbell.processor :as processor]
            [weepinbell.handler :refer [app]]
            [weepinbell.config :refer [load-config]]
            [weepinbell.webhook :as webhook]
            [taoensso.timbre :as timbre]
            [weepinbell.storage.edn-storage :as edn]
            [clojure.core.async :refer [chan]]
            [weepinbell.spec])
  (:gen-class)) ;; Essential for "lein uberjar"

(def wmc (chan))

(def config (load-config))

(def store (s/conform :weepinbell.spec/storage
                      (edn/use-store (:data-path config))))

(def hooks (let [hooks [:on-receive :on-update :on-delete :on-create]
                 webhooks (:webhook config)]
             (zipmap hooks
                     (map (fn [hook]
                            #(do (timbre/info (name hook) "callback triggered.")
                                 (webhook/trigger hook webhooks)))
                          hooks))))

(defn -main [& _]
  ;; Start webmention processor
  (processor/start wmc hooks store config)
  ;; Start receiver server
  ;; Check for a PORT environment variable, default to 3000
  (let [port (Integer/parseInt (or (:port config) (System/getenv "PORT") "3000"))]
    (run-server (app wmc store config) {:port port :join? true})))

