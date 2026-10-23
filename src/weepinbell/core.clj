(ns weepinbell.core
  "Main component that loads resources and start other components."
  (:require [weepinbell.storage.spec]
            [weepinbell.config :refer [load-config]]
            [weepinbell.webhook :as webhook]
            [weepinbell.storage.edn-storage :as edn]
            [weepinbell.storage.h2 :as h2]
            [weepinbell.storage.worker :as worker]
            [clojure.core.async :refer [chan >!! <!!]]
            [taoensso.timbre :as timbre]
            [nrepl.server :as nrepl]
            [weepinbell.migration :as m]
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
  [config]
  {:post [fn?]}
  (let [;; webmention channel 
        ;; Receiver puts webmention requests in. 
        ;; Processor takes them out.
        wmc (chan)
        ;; Storage middle layer, a map of data-manipulating functions.
        ;; Supports various storage as long as the map conforms storage.spec.
        data-path (:data-path config)
        bare-store (case (:storage-type config)
                     :edn (edn/use-store data-path)
                     :h2 (h2/use-store data-path))
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
      (stop-webserver) ;; stop receiving webmentions
      (stop-processor) ;; stop processing webmentions
      (stop-worker)    ;; stop writing to storage
      true)))

; Entry point; Terminal user interface
;; start up system and wait for signals to perform graceful shutdown
(defn start-engine []
  (let [config-file (or (System/getenv "WEEPINBELL_CONFIG") "config.edn")
        config (load-config config-file)
        log-level (or (System/getenv "WEEPINBELL_LOGLEVEL") 
                      (:log-level config) :info)
        nrepl-port (:nrepl-port config)]
    (timbre/merge-config! {:min-level log-level})
    
    (let [done-chan (chan 1)
          stop-system (do (timbre/info "starting weepinbell...")
                          (start-system config))
          nrepl-server (when nrepl-port
                         (timbre/info "starting nREPL server on port" nrepl-port)
                         (nrepl/start-server :port nrepl-port))]
      ;; Register signal handlers for SIGINT and SIGTERM
      (doseq [signal ["INT" "TERM"]]
        (Signal/handle (Signal. signal)
                       (reify SignalHandler
                         (handle [_ sig]
                           (stop-system)
                           (when nrepl-server
                             (nrepl/stop-server nrepl-server))
                           (timbre/info "signal" (.getName sig) "received."
                                        "gracefully shut down weepinbell.")
                           (>!! done-chan true)))))
      ;; block til signal received
      (<!! done-chan))))

(defn -main [& args]
  (case (first args)
    "migrate-db" (apply m/edn->h2 (rest args))
    (start-engine)))

