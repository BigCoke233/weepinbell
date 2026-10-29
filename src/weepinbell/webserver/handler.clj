(ns weepinbell.webserver.handler
  "Handler define routes and wrap responses. It pass request to other specific handlers.
  Other handlers are receiver, retriever and feed."
  (:require [clojure.spec.alpha :as s]
            [compojure.core :refer [routes POST GET context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
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
       ;; TODO /api/send is temporarily an alias of /endpoint 
       ;; will be a full webmention sender in the future
       (POST "/send" [source target]
         (receive {:s source :t target} wmc config))
     ;; query webmention list
       (context "/mentions" [page jf2type]
         (GET "/" [url]
           (retrieve-by-url url page store
                            {:type jf2type}))
         (GET "/:domain" [domain]
           (retrieve-by-domain domain page store
                               {:type jf2type})))
      ;; query count of webmention list 
       (context "/count" [jf2type]
         (GET "/" [url]
           (count-by-url url store {:type jf2type}))
         (GET "/:domain" [domain]
           (count-by-domain domain store {:type jf2type})))
      ;; query one specific webmention
       (context "/mention" []
         (GET "/:id" [id] (retrieve-by-id id store))
         (GET "/" [source target] (retrieve-by-pair source target store))))
    ;; RSS feed
     (GET "/feed/:domain" [domain] (gen-feed domain store config))
    ;; host proxied avatars (jf2 photos)
     (route/files "/avatars/" {:root "data/avatars"})
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
  (-> (make-api api-meta)
      make-handler
      (wrap-json-response)
      (wrap-defaults api-defaults)))

