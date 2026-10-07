(ns weepinbell.webserver.handler
  "Handler define routes and wrap responses. It pass request to other specific handlers.
  Other handlers are receiver, retriever and feed."
  (:require [clojure.spec.alpha :as s]
            [compojure.core :refer [routes POST GET context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.json :refer [wrap-json-response]]))

(s/def ::services
  (s/and (s/keys :req-un [::receive ::retrieve-by-url ::retrieve-by-domain
                          ::retrieve-by-id ::retrieve-by-pair ::gen-feed
                          ::count-by-domain ::count-by-url])
         #(every? fn? (vals %))))
(s/def ::api-meta
  (s/keys :req-un [::wmc ::store ::config ::services]))

(defn- make-api
  "Receives webmention channel, storage vtable and config map.
   Returns a vector of compojure route handlers. Some may be nil."
  [api-meta]
  {:pre [(s/valid? ::api-meta api-meta)]
   :post [sequential?]}
  (let [{:keys [wmc store config services]} api-meta
        {:keys [receive retrieve-by-url retrieve-by-domain
                retrieve-by-id retrieve-by-pair
                count-by-domain count-by-url gen-feed]} services]
    [;; receiver endpoint
     (POST (:endpoint config) [source target]
       (receive {:s source :t target} wmc config))
    ;; query API routes
     (context "/api" []
     ;; query webmention list
       (context "/mentions" [page microformats mftype]
         (GET "/" [url]
           (retrieve-by-url url page store
                            {:microformats microformats
                             :type mftype}))
         (GET "/:domain" [domain]
           (retrieve-by-domain domain page store
                               {:microformats microformats
                                :type mftype})))
      ;; query count of webmention list 
       (context "/count" [mftype]
         (GET "/" [url]
           (count-by-url url store {:type mftype}))
         (GET "/:domain" [domain]
           (count-by-domain domain store {:type mftype})))
      ;; query one specific webmention
       (context "/mention" [microformats]
         (GET "/:id" [id] (retrieve-by-id id store
                                          {:microformats microformats}))
         (GET "/" [source target] (retrieve-by-pair source target store
                                                    {:microformats microformats}))))
    ;; RSS feed
     (GET "/feed/:domain" [domain] (gen-feed domain store config))
    ;; host proxied avatars (microformats photos)
     (route/files "/avatars/" {:root "data/avatars"})
    ;; frontend
     (when-let [fe (:frontend config)]
       (when (= (:source fe) :disk)
         (route/files "/" {:root (:dir fe)})))
    ;; 404
     (route/not-found "Not Found")]))

(defn- make-handler
  "Receive a vector of handlers. Remove nil elements.
   Combine them in one handler using compojure routes."
  [api]
  (->> api (remove nil?) (apply routes)))

(defn app
  "Receive webmention channel, storage vtable and config map. 
   Returns an app handler."
  [api-meta]
  (when (= :clojure.spec.alpha/invalid (:store api-meta))
    (println "Invalid storage. Check your database configuration or submit an issue.")
    (System/exit 1))
  (-> (make-api api-meta)
      make-handler
      (wrap-resource "static")
      (wrap-defaults api-defaults)
      (wrap-json-response)))

