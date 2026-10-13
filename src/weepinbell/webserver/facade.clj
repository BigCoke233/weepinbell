(ns weepinbell.webserver.facade
  (:require [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :as timbre]
            [clojure.spec.alpha :as s]
            [weepinbell.webserver.handler :refer [app]]
            ;; services
            [weepinbell.webserver.receiver :as receiver]
            [weepinbell.webserver.retriever :as retriever]
            [weepinbell.webserver.feed :as feed]
            [weepinbell.webserver.counter :as counter]))

(s/def ::server-meta
  (s/keys :req-un [::wmc ::store ::config]))

(defn make-services []
  {:post [#(s/valid? :weepinbell.webserver.handler/services %)]}
  {:gen-feed feed/rss
   :receive receiver/receive
   :retrieve-by-domain retriever/by-domain
   :retrieve-by-url retriever/by-url
   :retrieve-by-id retriever/by-id
   :retrieve-by-pair retriever/by-st-pair
   :count-by-domain counter/by-domain
   :count-by-url counter/by-url})

(defn start-webserver
  "Starts web server. Returns a function that stops the server."
  [metadata]
  {:pre [(s/valid? ::server-meta metadata)]
   :post [fn?]}
  (let [{:keys [config]} metadata
        api-meta (assoc metadata :services (make-services))
        port (Integer/parseInt (or (:port config) (System/getenv "PORT") "3000"))]
    (timbre/info "starting web server, listening on" port)
    (run-server (app api-meta) {:port port})))

