(ns weepinbell.core
  "Main component that loads resources and start other components."
  (:require [weepinbell.storage.spec]
            [weepinbell.config :refer [load-config]]
            [weepinbell.webhook :as webhook]
            [weepinbell.storage.edn-storage :as edn]
            [weepinbell.storage.worker :as worker]
            [clojure.core.async :refer [chan >!! <!!]]
            [taoensso.timbre :as timbre]
            ;; two main subsystems' facades
            [weepinbell.processor.facade :refer [start-processor]]
            [weepinbell.webserver.facade :refer [start-webserver]])
  (:import (sun.misc Signal SignalHandler))
  (:gen-class))

; Handle startup; Callable from REPL
;; start whole system
(defn start-system
  "Start webmention processor and webserver in the background.
   Returns a function that performs a graceful shutdown."
  []
  {:post [fn?]}
  (let [;; webmention channel 
        ;; Receiver puts webmention requests in. 
        ;; Processor takes them out.
        wmc (chan)
        ;; Weepinbell configuration, 
        ;; loaded from config.edn and merged with defaults.
        config (load-config (or (System/getenv "WEEPINBELL_CONFIG") "config.edn"))
        ;; Storage middle layer, a map of data-manipulating functions.
        ;; Supports various storage as long as the map conforms storage.spec.
        bare-store (edn/use-store (:data-path config))
        [stop-worker async-store] (worker/start bare-store)
        ;; Hooks, a map of callback functions that processor calls 
        ;; on certain events. Can be used for webhooks and more.
        hooks (webhook/make-hooks config)
        ;; start processor and webservers and get stoppers 
        stop-processor (start-processor
                        {:wmc wmc
                         :callbacks hooks
                         :storage async-store
                         :config config})
        stop-webserver (start-webserver
                        {:wmc wmc
                         :store async-store
                         :config config})]
    (fn [] 
      (stop-processor) 
      (stop-worker)
      (stop-webserver) 
      true)))

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

