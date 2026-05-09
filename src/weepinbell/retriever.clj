(ns weepinbell.retriever
  (:require [weepinbell.storage :as storage]
            [weepinbell.utils :refer [get-domain]]
            [weepinbell.microformats-wrapper :as mf-wrapper]))

(def default-page-size 12)

;; utilities
(defn- partition-pages
  [page size data]
  (let [pages (partition-all size data)
        length (count pages)
        page (cond (keyword? page) page
                   (string? page)  (Integer/parseInt page)
                   :else           false)]
    (cond (= page :last) (last pages)
          (int? page)    (if (>= page length) false
                             (nth pages page))
          (not page)     false
          :else          false)))

(defn- respond-webmention-json [data]
  (let [base-resp {:status 200
                   :headers {"Content-Type" "application/json; charset=utf-8"}
                   :body nil}]
    (if data
      (assoc base-resp :body data)
      (-> base-resp
        (assoc :status 400)
        (assoc :body "The requested webmention does not exist.")))))

(defn- respond-webmention-list-json [data]
  (let [base-resp {:status 200
                   :headers {"Content-Type" "application/json; charset=utf-8"}
                   :body nil}]
    (if data
      (assoc base-resp :body data)
      (-> base-resp
        (assoc :status 400)
        (assoc :body {:message "The list is empty, something is wrong with your request."})))))

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

(defn- get-mftype-key [typ]
  (get {:like :like-of
        :repost :repost-of
        :reply :in-reply-to
        :rsvp :rsvp} typ))

(defn- filter-response-by-mftype [typ data]
  (when typ
    (filter #(= (-> % :microformats :type)
                (get-mftype-key typ))
            data)))
;; end utilities

(defn by-domain
  "Get webmentions sent to a particular domain."
  [domain typ page]
  (cond->> (storage/get-wms-by-domain domain)
    true wrap-response-list
    typ  (filter-response-by-mftype typ)
    page (partition-pages page default-page-size)
    true respond-webmention-list-json))

(defn by-url
  "Get webmention sent to a particular URL."
  [url typ page]
  (if url
    (cond->> (get-domain url)
      true storage/get-wms-by-domain
      true (filter #(= url (get-in % [:webmention :target])))
      true wrap-response-list
      typ  (filter-response-by-mftype typ)
      page (partition-pages page default-page-size)
      true respond-webmention-list-json)
    ;; if no url query is passed
    {:status 400
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body {:status 400
            :message "Missing URL. Specify target URL whose webmentions you are querying."}}))

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

