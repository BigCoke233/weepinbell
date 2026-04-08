(ns weepinbell.handler
  (:require [compojure.core :refer [defroutes POST GET context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.retriever :as retriever]
            [weepinbell.processor :refer [process-webmention]]
            [weepinbell.ui :as ui]
            [weepinbell.feed :as feed]
            [clojure.core.async :refer [chan]]))

(def wmc (chan))

(process-webmention wmc)

(defroutes api-routes
  (GET "/" [target] (ui/home (get-config :allowed-target-domains)
                       (get-config :endpoint)
                       target))
  ;; receiver endpoint
  (POST (get-config :endpoint) [source target]
    (receive {:s source :t target} wmc))
  ;; query API routes
  (context "/api" []
    ;; query webmention list
    (context "/mentions" []
      (GET "/" [url] (retriever/by-url url))
      (GET "/:domain" [domain] (retriever/by-domain domain)))
    ;; query one specific webmention
    (context "/mention" []
      (GET "/:id" [id] (retriever/by-id id))
      (GET "/" [source target] (retriever/by-st-pair source target))))
  ;; RSS feed
  (GET "/feed/:domain" [domain] (feed/rss domain))
  ;; host proxied avatars (microformats photos)
  (route/files "/avatars/" {:root "data/avatars"})
  ;; 404
  (route/not-found "Not Found"))

(def app
  (-> api-routes
      (wrap-resource "static")
      (wrap-defaults api-defaults)
      (wrap-content-type)))

