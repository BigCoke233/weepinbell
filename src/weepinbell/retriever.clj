(ns weepinbell.retriever
  (:require [weepinbell.storage :as storage]
            [weepinbell.utils :refer [valid-http-url? get-domain get-target]]
            [clojure.string :refer [includes?]]
            [cheshire.core :as json]))

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
    ;; responsd with data filtered and converted to json
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/generate-string (filter pred data))}))

