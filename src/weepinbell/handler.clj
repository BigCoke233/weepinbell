(ns weepinbell.handler
  (:require [compojure.core :refer [routes POST GET context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.json :refer [wrap-json-response]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.retriever :as retriever]
            [weepinbell.ui :as ui]
            [weepinbell.feed :as feed]))

;; Define API routes

(defmacro defmention-routes [base-path mention-type storage]
  `(context ~base-path []
     (GET "/" [~'url ~'page]
       (retriever/by-url ~'url ~mention-type ~'page ~storage))
     (GET "/:domain" [~'domain ~'page]
       (retriever/by-domain ~'domain ~mention-type ~'page ~storage))))

(defn api-routes [wmc storage config]
  (routes
   (GET "/" [target] (ui/home (:allowed-target-domains config)
                              (:endpoint config)
                              target))
    ;; receiver endpoint
   (POST (:endpoint config) [source target]
     (receive {:s source :t target} wmc config))
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
   (GET "/feed/:domain" [domain] (feed/rss domain storage config))
    ;; host proxied avatars (microformats photos)
   (route/files "/avatars/" {:root "data/avatars"})
    ;; 404
   (route/not-found "Not Found")))

(defn app [wmc storage config]
  (when (= :clojure.spec.alpha/invalid storage)
    (println "Invalid storage. Check your database configuration or submit an issue.")
    (System/exit 1))
  (-> (api-routes wmc storage config)
      (wrap-resource "static")
      (wrap-defaults api-defaults)
      (wrap-json-response)))

