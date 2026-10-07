(ns weepinbell.webserver.facade
  (:require [org.httpkit.server :refer [run-server]]
            [weepinbell.webserver.handler :refer [app]]
            [taoensso.timbre :as timbre]
            [clojure.spec.alpha :as s]))

(s/def ::server-meta
  (s/keys :req-un [::wmc ::store ::config]))

(defn start-webserver
  "Starts web server. Returns a function that stops the server."
  [metadata]
  {:pre [(s/valid? ::server-meta metadata)]
   :post [fn?]}
  (let [{:keys [wmc store config]} metadata
        port (Integer/parseInt (or (:port config) (System/getenv "PORT") "3000"))]
    (timbre/info "starting web server, listening on" port)
    (run-server (app wmc store config) {:port port})))

