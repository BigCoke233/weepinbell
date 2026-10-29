(ns weepinbell.webserver.retriever
  "Service getting webmention form storage and wrapping it in a response."
  (:require [weepinbell.utils.url :refer [get-domain get-path]]
            [ring.util.http-response :refer [bad-request not-found ok]]))

(def default-page-size 12)

;; response utilities 

(defn- respond-webmention-json [data]
  (if (and data (some? (:webmention data)))
    (ok data)
    (not-found {:message "The requested webmention does not exist."})))

(defn- respond-webmention-list-json [data]
  (cond
    (= data :OOB) (bad-request {:message "Page number is too large."})
    (empty? data) (not-found {:message "The requested webmention list does not exist."})
    :else         (ok data)))

;; query list of webmentions

(defn- filter-by-jf2type
  [jf2type data]
  (if (nil? jf2type) data
      (filter #(= jf2type (get-in % [:jf2 :wm-property])) data)))

(defn- paging
  "Paging utility, given a full set of data, partition it and return
   the requested frame.
   Returns the last frame if page number is not a number, as a fallback.
   Returns :OOB (Out Of Bounds) if page number is too large."
  [^Long page ^Long page-size data]
  (if (nil? page)
    ;; if no page number, return the whole query-list
    data
    ;; otherwise, partition all and return the requested frame
    (let [pages (->> data (partition-all page-size) vec)
          npages (count pages)
          page-number (if (number? page) page
                          (- (count pages) 1))]
      (if (>= page-number npages)
        :OOB ; Out Of Bounds
        (get pages page-number)))))

(defn query-list
  "Get a list of webmentions by certain filters."
  [{:keys [^String domain, ^String path, ^Long page, ^Long page-size
           storage mftype]}]
  {:pre [(contains? storage :get-by-domain)]}
  (let [;; parse to number if possible, use original if not
        page (try (Long/parseLong page) (catch Exception _ page))
        ;; data trasformations
        data ((:get-by-domain storage path) domain)
        data (filter-by-jf2type mftype data)
        data (paging page page-size data)]
    (respond-webmention-list-json data)))

(defn by-domain
  "Get webmentions sent to a particular domain."
  [domain page storage opts]
  (query-list {:domain domain
               :path nil
               :page page
               :page-size default-page-size
               :storage storage
               :mftype (:type opts)}))

(defn by-url
  "Get webmention sent to a particular URL."
  [url page storage opts]
  (if url
    (query-list {:domain (get-domain url)
                 :path (get-path url)
                 :page page
                 :page-size default-page-size
                 :storage storage
                 :mftype (:type opts)})
    {:status 400
     :body {:message "Missing URL. Specify target URL whose webmentions you are querying."}}))

;; query one webmention

(defn query-one
  "Get one specific webmention by id or by its source and target."
  [opts]
  {:post [(vector? (:identifiers opts))
          (contains? (:storage opts) :get)]}
  (let [{:keys [identifiers storage]} opts
        data (apply (:get storage) identifiers)]
    (respond-webmention-json data)))

(defn by-id
  "Get one specific webmention by ID."
  [id storage]
  (query-one {:identifiers [id]
              :storage storage}))

(defn by-st-pair
  "Get one specific webmention by source and target."
  [source target storage]
  (query-one {:identifiers [source target]
              :storage storage}))

