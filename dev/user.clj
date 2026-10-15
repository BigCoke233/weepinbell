(ns user
  (:require [clj-reload.core :as reload]
            [nextjournal.beholder :as beholder]
            [weepinbell.config :refer [load-config]]
            [weepinbell.core]))

(reload/init
 {:dirs ["src" "dev" "test"]})

(defonce stop-fn (atom nil))

(defn start []
  {:pre [(nil? @stop-fn)]}
  (reset! stop-fn 
          (@(resolve 'weepinbell.core/start-system)
           (load-config "config.edn"))))

(defn stop []
  {:pre [(fn? @stop-fn)]}
  (@stop-fn)
  (reset! stop-fn nil))

(defn restart []
  (stop)
  (reload/reload)
  (start))

(defn watch []
  (beholder/watch (fn [_] ((requiring-resolve 'user/restart))) "src"))

