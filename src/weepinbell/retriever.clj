(ns weepinbell.retriever
  "Handler of routes /api/mention/:id, /api/mentions/...
  Retriever gets webmention form storage and wraps it in a response."
  (:require [weepinbell.utils.url :refer [get-domain]]
            [weepinbell.microformats.reducer :refer [reduce-mf]]))

(def default-page-size 12)

;; utilities
(defn- partition-pages
  [page size data]
  (let [pages (partition-all size data)
        length (count pages)
        page (cond (keyword? page) page
                   (= page "")     0
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
    (if (and data (some? (:webmention data)))
      (assoc base-resp :body data)
      (-> base-resp
          (assoc :status 404)
          (assoc :body {:message "The requested webmention does not exist."})))))

(defn- respond-webmention-list-json [data]
  (let [base-resp {:status 200
                   :headers {"Content-Type" "application/json; charset=utf-8"}
                   :body nil}]
    (if data
      (assoc base-resp :body data)
      (-> base-resp
          (assoc :status 404)
          (assoc :body {:message "The requested webmention list does not exist."})))))

(defn- wrap-response-object [data opts]
  (let [result {:webmention (:webmention data)}]
    (cond-> result
      (not (empty? (:microformats data)))
      (assoc :microformats (cond-> data 
                             true :microformats 
                             (not= "raw" (:microformats opts)) reduce-mf))
      (:extra data)
      (assoc :extra (:extra data)))))

(defn- wrap-response-list [opts data]
  (map #(wrap-response-object % opts) data))

(defn- filter-response-by-mftype [typ data]
  (filter #(= typ (get-in % [:microformats :type])) data))
;; end utilities

(defn by-domain
  "Get webmentions sent to a particular domain."
  [domain page storage opts]
  (cond->> ((:get-by-domain storage) domain)
    true (wrap-response-list opts)
    (:type opts) (filter-response-by-mftype (:type opts))
    page (partition-pages page default-page-size)
    true respond-webmention-list-json))

(defn by-url
  "Get webmention sent to a particular URL."
  [url page storage opts]
  (if url
    (cond->> (get-domain url)
      true ((:get-by-domain storage))
      true (filter #(= url (get-in % [:webmention :target])))
      true (wrap-response-list opts)
      (:type opts) (filter-response-by-mftype (:type opts))
      page (partition-pages page default-page-size)
      true respond-webmention-list-json)
    ;; if no url query is passed
    {:status 400
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body {:status 400
            :message "Missing URL. Specify target URL whose webmentions you are querying."}}))

(defn by-id
  "Get one specific webmention by ID."
  [id storage opts]
  (-> ((:get storage) id)
      (wrap-response-object opts)
      respond-webmention-json))

(defn by-st-pair
  "Get one specific webmention by source and target."
  [source target storage opts]
  (-> ((:get storage) source target)
      (wrap-response-object opts)
      respond-webmention-json))

