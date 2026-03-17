(ns weepinbell.retriever
  (:require [weepinbell.storage :as storage]
            [weepinbell.utils :refer [valid-http-url?
                                      get-domain
                                      get-path
                                      get-target
                                      respond-json]]
            [clojure.string :refer [includes?]]
            [cheshire.core :as json]))

;; utilities
(defn- respond-webmention-json [data]
  (if data
    (respond-json data)
    (respond-json {:status 404
                   :message "Webmention does not exist or is pending for verification."}
                  404)))
;; end utilities

(defn all
  "Get all webmentions."
  [filter-string]
  (let [data (storage/load-data)
        has-filter? (not (nil? filter-string))
        filter-by-target? (and has-filter? (valid-http-url? filter-string))
        filter-by-domain? (and has-filter? (includes? filter-string "."))
        ;; determine filter predicate
        pred (cond
               filter-by-target? #(= filter-string (get-target %))
               filter-by-domain? #(= filter-string (-> % get-target get-domain))
               :else             identity)] ;; Use identity instead of not-empty for "no filter"
    (respond-json (filter pred data))))

(defn by-domain
  "Get webmentions sent to a particular domain."
  [domain]
  (let [data (storage/get-wms-by-domain domain)]
    (respond-json data)))

(defn by-domain-path
  "Get webmention sent to a particular path on a particular domain. More flexible than bare URL."
  [domain path]
  (let [data (storage/get-wms-by-domain domain path)]
    (respond-json data)))

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
  (respond-webmention-json (storage/get-wm id)))

(defn by-st-pair
  "Get one specific webmention by source and target."
  [source target]
  (respond-webmention-json (storage/get-wm source target)))

