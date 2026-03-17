(ns weepinbell.handler
  (:require [compojure.core :refer [defroutes POST GET]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.retriever :as retriever]
            [weepinbell.processor :refer [process-webmention]]
            [clojure.core.async :refer [chan]]))

(def wmc (chan))

(process-webmention wmc)

(defroutes api-routes
  ;; receiver endpoint
  (POST (get-config :endpoint) [source target]
    (receive {:s source :t target} wmc))
  ;; query webmentions
  (GET "/api/mentions/" [url] (retriever/by-url url))
  (GET "/api/mentions/:domain" [domain] (retriever/by-domain domain))
  (GET "/api/mentions/:domain/:path" [domain path] (retriever/by-domain-path domain path))
  (GET "/api/mentions/all" [filter] (retriever/all filter))
  ;; query one specific webmention
  (GET "/api/mention/:id" [id] (retriever/by-id id))
  (GET "/api/mention" [source target] (retriever/by-st-pair source target))
  ;; host proxied avatars (microformats photos)
  (route/files "/avatars/" {:root "data/avatars"})
  ;; 404
  (route/not-found "Not Found"))

(def app
  (wrap-defaults api-routes api-defaults))
