(ns weepinbell.webserver.retriever
  "Service getting webmention form storage and wrapping it in a response."
  (:require [weepinbell.utils.url :refer [get-domain get-path]]
            [weepinbell.microformats.reducer :refer [reduce-mf]]
            [weepinbell.microformats.searcher :as mfsr]
            [ring.util.http-response :refer [bad-request not-found ok]]
            [clojure.spec.alpha :as s]))

(def default-page-size 12)

;; response utilities 

(defn- respond-webmention-json [data]
  (if (and data (some? (:webmention data)))
    (ok data)
    (not-found {:message "The requested webmention does not exist."})))

(defn- respond-webmention-list-json [data]
  (cond
    (false? data) (bad-request {:message "Page number is invalid. Either too large or not a number."})
    (empty? data) (not-found {:message "The requested webmention list does not exist."})
    :else         (ok data)))

;; query list of webmentions

(defn mftype= [& types]
  (apply = (map keyword types)))
(defn filter-by-mftype
  [mftype data]
  (if (nil? mftype) data
      (->> data
           (map (fn [item]
                  [(get-in item [:webmention :target])
                   item]))
           (filter (fn [[target item]]
                     (->> item  
                          :microformats
                          mfsr/use-items
                          mfsr/find-h-entry
                          (mfsr/search-mftype target)
                          (mftype= mftype))))
           (map last))))

(defn query-list
  "Get a list of webmentions by certain filters."
  {:pre [(s/keys :req-un [::domain ::path ::page ::page-size ::storage ::mftype])]}
  [opts]
  (let [{:keys [domain path page page-size storage mftype]} opts
        reduce-mf? (not= (:microformats opts) "raw")
        page-size (or page-size default-page-size)
        data ((:get-by-domain storage path) domain)
        data (filter-by-mftype mftype data)
        pages (vec (partition-all page-size data))
        page-number (if (number? page) page
                        (- (count pages) 1))
        page-selected (get pages page-number)
        result (if reduce-mf? (map #(assoc % :microformats
                                           (reduce-mf (get-in % [:webmention :target])
                                                      (:microformats %)))
                                   page-selected)
                   page-selected)]
    (respond-webmention-list-json result)))

(defn by-domain
  "Get webmentions sent to a particular domain."
  [domain page storage opts]
  (query-list {:domain domain
               :path nil
               :page page
               :page-size default-page-size
               :storage storage
               :mftype (:type opts)
               :microformats (:microformats opts)}))

(defn by-url
  "Get webmention sent to a particular URL."
  [url page storage opts]
  (if url
    (query-list {:domain (get-domain url)
                 :path (get-path url)
                 :page page
                 :page-size default-page-size
                 :storage storage
                 :mftype (:type opts)
                 :microformats (:microformats opts)})
    {:status 400
     :body {:message "Missing URL. Specify target URL whose webmentions you are querying."}}))

;; query one webmention

(defn query-one
  "Get one specific webmention by id or by its source and target."
  [opts]
  {:post [(s/keys :req-un [::identifiers ::storage ::microformats])]}
  (let [{:keys [identifiers storage microformats]} opts
        reduce-mf? (not= "raw" microformats)
        data (apply (:get storage) identifiers)
        result (if (and (:microformats data) reduce-mf?)
                 (assoc data :microformats
                        (reduce-mf (get-in (:microformats data) [:webmention :target])
                                   (:microformats data)))
                 data)]
    (respond-webmention-json result)))

(defn by-id
  "Get one specific webmention by ID."
  [id storage opts]
  (query-one {:identifiers [id]
              :storage storage
              :microformats (:microformats opts)}))

(defn by-st-pair
  "Get one specific webmention by source and target."
  [source target storage opts]
  (query-one {:identifiers [source target]
              :storage storage
              :microformats (:microformats opts)}))

