(ns weepinbell.handler
  (:require [compojure.core :refer [defroutes POST GET context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.retriever :as retriever]
            [weepinbell.ui :as ui]
            [weepinbell.feed :as feed]
            [clojure.core.async :refer [chan]]))

(def wmc (chan))

;; Define API routes

(defmacro defmention-routes [base-path mention-type]
  `(context ~base-path []
     (GET "/" [~'url] (retriever/by-url ~'url ~mention-type))
     (GET "/:domain" [~'domain] (retriever/by-domain ~'domain ~mention-type))))

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
    (defmention-routes "/mentions" nil)
    (defmention-routes "/likes" :like)
    (defmention-routes "/reposts" :repost)
    (defmention-routes "/replies" :reply)
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
      (wrap-json-response)
      (wrap-json-body)))

