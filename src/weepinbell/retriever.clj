(ns weepinbell.retriever
  (:require [weepinbell.storage :as storage]
            [weepinbell.utils :refer [valid-http-url? get-domain get-target respond-json]]
            [clojure.string :refer [includes?]]))


(defn retrieve
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

(defn retrieve-by-id
  "Get one specific webmention by ID."
  [id]
  (if-let [wm (storage/get-wm id)]
     (respond-json wm)
     (respond-json {:message "webmention not found"} 404)))

