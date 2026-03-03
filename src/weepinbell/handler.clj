(ns weepinbell.handler
  (:require [compojure.core :refer [defroutes POST]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.processor :refer [process-webmention]]
            [clojure.core.async :refer [chan]]))

(def wmc (chan))

(process-webmention wmc)

(defroutes api-routes
  (POST (get-config :endpoint) [source target]
    (receive {:s source :t target} wmc))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults api-routes api-defaults))
