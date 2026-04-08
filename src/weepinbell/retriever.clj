(ns weepinbell.retriever
  (:require [weepinbell.storage :as storage]
            [weepinbell.utils :refer [valid-http-url?
                                      get-domain
                                      get-path
                                      get-target
                                      respond-json]]
            [weepinbell.microformats-wrapper :as mf-wrapper]
            [clojure.string :refer [includes?]]
            [cheshire.core :as json]))

;; utilities
(defn- respond-webmention-json [data]
  (if data
    (respond-json data)
    (respond-json {:status 404
                   :message "Webmention does not exist or is pending for verification."}
                  404)))

(defn- wrap-response-object [data]
  {:webmention (:webmention data)
   :microformats (mf-wrapper/wrap-entry (:microformats data))
   :extra (:extra data)})

(defn- wrap-response-list [data]
  (map wrap-response-object data))
;; end utilities

(defn by-domain
  "Get webmentions sent to a particular domain."
  [domain]
  (-> (storage/get-wms-by-domain domain)
      wrap-response-list
      respond-json))

(defn by-domain-path
  "Get webmention sent to a particular path on a particular domain. More flexible than bare URL."
  [domain path]
  (-> (storage/get-wms-by-domain domain path)
      wrap-response-list
      respond-json))

(defn by-url
  "Get webmention sent to a particular URL."
  [url]
  (if url
    (let [domain (get-domain url)
          path (get-path url)]
      (by-domain-path domain path))
    ;; if no url query is passed
    (respond-json {:status 400
                   :message "Missing URL. Specify target URL whose webmentions you are querying."}
                  400)))

(defn by-id
  "Get one specific webmention by ID."
  [id]
  (-> (storage/get-wm id)
      wrap-response-object
      respond-webmention-json))

(defn by-st-pair
  "Get one specific webmention by source and target."
  [source target]
  (-> (storage/get-wm source target)
      wrap-response-object
      respond-webmention-json))

