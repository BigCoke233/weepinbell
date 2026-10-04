(ns weepinbell.handler
  "Handler define routes and wrap responses. It pass request to other specific handlers.
  Other handlers are receiver, retriever and feed."
  (:require [compojure.core :refer [routes POST GET context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.json :refer [wrap-json-response]]
            [weepinbell.receiver :refer [receive]]
            [weepinbell.retriever :as retriever]
            [weepinbell.feed :as feed]))

;; Define API routes

(defn make-api [wmc storage config]
  [;; receiver endpoint
   (POST (:endpoint config) [source target]
     (receive {:s source :t target} wmc config))
    ;; query API routes
   (context "/api" []
     ;; query webmention list
     (context "/mentions" [page microformats mftype]
       (GET "/" [url]
         (retriever/by-url url page storage 
                           {:microformats microformats
                            :type mftype}))
       (GET "/:domain" [domain]
         (retriever/by-domain domain page storage 
                              {:microformats microformats
                               :type mftype})))
      ;; query one specific webmention
     (context "/mention" [microformats]
       (GET "/:id" [id] (retriever/by-id id storage 
                                         {:microformats microformats}))
       (GET "/" [source target] (retriever/by-st-pair source target storage
                                                      {:microformats microformats}))))
    ;; RSS feed
   (GET "/feed/:domain" [domain] (feed/rss domain storage config))
    ;; host proxied avatars (microformats photos)
   (route/files "/avatars/" {:root "data/avatars"})
    ;; frontend
   (when-let [fe (:frontend config)]
     (when (= (:source fe) :disk)
       (route/files "/" {:root (:dir fe)})))
    ;; 404
   (route/not-found "Not Found")])

(defn- make-handler [api]
  (->> api (remove nil?) (apply routes)))

(defn app [wmc storage config]
  (when (= :clojure.spec.alpha/invalid storage)
    (println "Invalid storage. Check your database configuration or submit an issue.")
    (System/exit 1))
  (-> (make-api wmc storage config)
      make-handler
      (wrap-resource "static")
      (wrap-defaults api-defaults)
      (wrap-json-response)))

