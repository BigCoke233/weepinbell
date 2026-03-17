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
      (GET "/:domain" [domain] (retriever/by-domain domain))
      (GET "/:domain/:path" [domain path] (retriever/by-domain-path domain path))
      (GET "/all" [filter] (retriever/all filter)))
    ;; query one specific webmention
    (context "/mention" []
      (GET "/mention/:id" [id] (retriever/by-id id))
      (GET "/mention" [source target] (retriever/by-st-pair source target))))
  ;; host proxied avatars (microformats photos)
  (route/files "/avatars/" {:root "data/avatars"})
  ;; 404
  (route/not-found "Not Found"))

(def app
  (-> api-routes
      (wrap-resource "static")
      (wrap-defaults api-defaults)
      (wrap-content-type)))

