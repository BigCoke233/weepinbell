(ns weepinbell.core
  "Main component that loads resources and start other components."
  (:require [org.httpkit.server :refer [run-server]]
            [clojure.spec.alpha :as s]
            [weepinbell.processor.starter :refer [start-processor]]
            [weepinbell.handler :refer [app]]
            [weepinbell.config :refer [load-config]]
            [weepinbell.webhook :as webhook]
            [weepinbell.storage.edn-storage :as edn]
            [clojure.core.async :refer [chan >!! <!!]]
            [taoensso.timbre :as timbre]
            [weepinbell.storage.spec])
  (:import (sun.misc Signal SignalHandler))
  (:gen-class)) ;; Essential for "lein uberjar"

; Handle startup; Callable from REPL
;; processor starter is in ns weepinbell.proceesor.starter
;; webserver starter is defined here, temporarily
(defn start-webserver
  "Starts web server. Returns a function that stops the server."
  [server-meta]
  (let [{:keys [wmc store config]} server-meta
        port (Integer/parseInt (or (:port config) (System/getenv "PORT") "3000"))]
    (timbre/info "starting web server, listening on" port)
    (run-server (app wmc store config) {:port port})))

;; start whole system
(defn start-system
  "Start webmention processor and webserver in the background.
   Returns a function that performs a graceful shutdown."
  []
  (let [;; webmention channel 
        ;; Receiver puts webmention requests in. 
        ;; Processor takes them out.
        wmc (chan)
        ;; Weepinbell configuration, 
        ;; loaded from config.edn and merged with defaults.
        config (load-config)
        ;; Storage middle layer, a map of data-manipulating functions.
        ;; Supports various storage as long as the map conforms storage.spec.
        store (s/conform :weepinbell.storage.spec/vtable
                         (edn/use-store (:data-path config)))
        ;; Hooks, a map of callback functions that processor calls 
        ;; on certain events. Can be used for webhooks and more.
        hooks (webhook/make-hooks config)
        ;; start processor and webservers and get stoppers 
        stop-processor (start-processor
                        {:wmc wmc
                         :callbacks hooks
                         :storage store
                         :config config})
        stop-webserver (start-webserver
                        {:wmc wmc
                         :store store
                         :config config})]
    (fn [] (stop-processor) (stop-webserver))))

; Entry point; Terminal user interface
;; start up system and wait for signals to perform graceful shutdown
(defn -main [& _]
  (timbre/info "starting weepinbell...")
  (let [done-chan (chan 1)
        stop-system (start-system)]
    ;; Register signal handlers for SIGINT and SIGTERM
    (doseq [signal ["INT" "TERM"]]
      (Signal/handle (Signal. signal) 
        (reify SignalHandler
          (handle [_ sig] 
            (stop-system)
            (timbre/info "signal" (.getName sig) "received." 
                         "gracefully shut down weepinbell.")
            (>!! done-chan true)))))
    ;; block til signal received
    (<!! done-chan)))

