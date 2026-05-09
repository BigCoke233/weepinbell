(ns weepinbell.handler
  (:require [compojure.core :refer [routes POST GET context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.json :refer [wrap-json-response]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.retriever :as retriever]
            [weepinbell.channel :refer [wmc]]
            [weepinbell.ui :as ui]
            [weepinbell.feed :as feed]))

;; Define API routes

(defmacro defmention-routes [base-path mention-type storage]
  `(context ~base-path []
     (GET "/" [~'url ~'page] 
          (retriever/by-url ~'url ~mention-type ~'page ~storage))
     (GET "/:domain" [~'domain ~'page] 
          (retriever/by-domain ~'domain ~mention-type ~'page ~storage))))

(defn api-routes [storage]
  (routes
    (GET "/" [target] (ui/home (get-config :allowed-target-domains)
                               (get-config :endpoint)
                               target))
    ;; receiver endpoint
    (POST (get-config :endpoint) [source target]
      (receive {:s source :t target} wmc))
    ;; query API routes
    (context "/api" []
      ;; query webmention list
      (defmention-routes "/mentions" nil storage)
      (defmention-routes "/likes" :like storage)
      (defmention-routes "/reposts" :repost storage)
      (defmention-routes "/replies" :reply storage)
      ;; query one specific webmention
      (context "/mention" []
        (GET "/:id" [id] (retriever/by-id id storage))
        (GET "/" [source target] (retriever/by-st-pair source target storage))))
    ;; RSS feed
    (GET "/feed/:domain" [domain] (feed/rss domain storage))
    ;; host proxied avatars (microformats photos)
    (route/files "/avatars/" {:root "data/avatars"})
    ;; 404
    (route/not-found "Not Found")))

(defn app [storage]
  (-> (api-routes storage)
      (wrap-resource "static")
      (wrap-defaults api-defaults)
      (wrap-json-response)))

