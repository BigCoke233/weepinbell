(ns weepinbell.retriever
  (:require [weepinbell.storage :as storage]
            [cheshire.core :as json]))

(defn retrieve-all
  "Get all webmentions."
  []
  (let [body (json/generate-string (storage/load-data))]
    {:status 200 :body body}))

