(ns weepinbell.retriever
  (:require [weepinbell.storage :as storage]
            [weepinbell.utils :refer [get-domain
                                      get-path
                                      respond-json]]
            [weepinbell.microformats-wrapper :as mf-wrapper]))

;; utilities
(defn- respond-webmention-json [data]
  (if data
    (respond-json data)
    (respond-json {:status 404
                   :message "Webmention does not exist or is pending for verification."}
                  404)))

(defn- wrap-response-object [data]
  (let [result {:webmention (:webmention data)}]
    (cond-> result
      (not (empty? (:microformats data)))
        (assoc :microformats (-> data
                                 :microformats
                                 mf-wrapper/wrap-entry))
      (:extra data)
        (assoc :extra (:extra data)))))

(defn- wrap-response-list [data]
  (map wrap-response-object data))
;; end utilities

(defn by-domain
  "Get webmentions sent to a particular domain."
  [domain]
  (-> (storage/get-wms-by-domain domain)
      wrap-response-list
      respond-json))

(defn by-url
  "Get webmention sent to a particular URL."
  [url]
  (if url
    (->> (get-domain url)
         storage/get-wms-by-domain
         (filter #(= url (get-in % [:webmention :target])))
         wrap-response-list
         respond-json)
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

