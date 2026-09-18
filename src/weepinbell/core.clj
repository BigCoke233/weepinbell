(ns weepinbell.core
  "Main component that loads resources and start other components."
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.spec.alpha :as s]
            [weepinbell.processor :as processor]
            [weepinbell.handler :refer [app]]
            [weepinbell.config :refer [load-config]]
            [weepinbell.webhook :as webhook]
            [weepinbell.storage.edn-storage :as edn]
            [clojure.core.async :refer [chan]]
            [taoensso.timbre :as timbre]
            [weepinbell.storage.spec])
  (:gen-class)) ;; Essential for "lein uberjar"

(def ^{:doc "Webmention channel.
       Receiver puts webmention requests in. Processor takes them out."}
  wmc (chan))

(def ^{:doc "Weepinbell configuration, 
       loaded from config.edn and merged with defaults."}
  config (load-config))

(def ^{:doc "Storage middle layer, a map of data-manipulating functions.
       Supports various storage as long as the map conforms storage.spec."}
  store (s/conform :weepinbell.storage.spec/vtable
                      (edn/use-store (:data-path config))))

(def ^{:doc "Hooks, a map of callback functions that processor calls 
       on certain events. Can be used for webhooks and more."}
  hooks (let [hooks [:on-receive :on-update :on-delete :on-create]
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
    (run-server (app wmc store config) {:port port :join? true})
    (timbre/info "weepinbell started, listening on" port)))

