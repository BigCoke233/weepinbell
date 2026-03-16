(ns weepinbell.handler
  (:require [compojure.core :refer [defroutes POST GET]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.retriever :refer [retrieve]]
            [weepinbell.processor :refer [process-webmention]]
            [clojure.core.async :refer [chan]]))

(def wmc (chan))

(process-webmention wmc)

(defroutes api-routes
  (POST (get-config :endpoint) [source target]
    (receive {:s source :t target} wmc))
  (GET "/api/mentions" [filter] (retrieve filter))
  (route/files "/avatars/" {:root "data/avatars"})
  (route/not-found "Not Found"))

(def app
  (wrap-defaults api-routes api-defaults))
