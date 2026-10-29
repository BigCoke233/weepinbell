(ns weepinbell.metadata.json-ld
  "JSON-LD parser that finds JSON-LD string in a hickroy document
   and then tries to convert it to a JF2 object."
  (:require [weepinbell.metadata.html-head :refer [get-child]]
            [cheshire.core :as json]))

(defn- find-json-ld [hickory]
  (->> hickory (get-child :html) (get-child :head) :content
       (filter #(and (= (:tag %) :script)
                     (= "application/ld+json"
                        (get-in % [:attrs :type]))))
       first :content first))

(defn parse-json-ld
  "Takes hickory document, finds <script type=\"application/ld+json\">,
   parses JSON, returns a JF2 map."
  [hickory]
  (let [data (-> hickory find-json-ld json/parse-string)
        author (get data "author")]
    {:type "entry"
     :name (or (get data "headline")
               (get data "name"))
     :url (get data "url")
     :author (when author
               {:type "card"
                :name (get author "name")
                :url (get author "url")
                :photo (get-in author ["image" "url"])})
     :published (get data "datePublished")
     :content ""
     :wm-property :mention
     :summary (get data "description")}))

