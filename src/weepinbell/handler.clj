(ns weepinbell.handler
  (:require [compojure.core :refer [defroutes POST GET]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.retriever :refer [retrieve retrieve-by-id retrieve-by-url]]
            [weepinbell.processor :refer [process-webmention]]
            [clojure.core.async :refer [chan]]))

(def wmc (chan))

(process-webmention wmc)

(defroutes api-routes
  ;; receiver endpoint
  (POST (get-config :endpoint) [source target]
    (receive {:s source :t target} wmc))
  ;; query webmentions
  (GET "/api/mentions" [filter] (retrieve filter))
  (GET "/api/mention/:id" [id] (retrieve-by-id id))
  (GET "/api/mention" [source target] (retrieve-by-url source target))
  ;; host proxied avatars (microformats photos)
  (route/files "/avatars/" {:root "data/avatars"})
  ;; 404
  (route/not-found "Not Found"))

(def app
  (wrap-defaults api-routes api-defaults))
