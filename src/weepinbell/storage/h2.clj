(ns weepinbell.storage.h2
  (:require [clojure.string :refer [blank? split]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :refer [as-unqualified-lower-maps]]
            [next.jdbc.sql :as sql]
            [clojure.java.io :as io]
            [fast-edn.core :as edn]
            [weepinbell.utils.url :refer [get-domain get-path]]
            [weepinbell.microformats.searcher :refer [use-items find-h-entry search-mftype]]))

;; data trasnformation utilities

(defn schema->map
  [{:keys [wm_source wm_target hash lastmod]} mf-row photo-row]
  (let [parse #(if (string? %) (edn/read-string %) %)
        mf (parse (:mf_data mf-row))
        photo (:url photo-row)]
    (cond-> {:webmention {:source wm_source
                          :target wm_target
                          :id hash
                          :modified lastmod}}
      (some? mf)    (assoc :microformats mf)
      (some? photo) (assoc :extra {:proxied-photo photo}))))

(defn wm->schema [data]
  (let [{:keys [webmention]} data
        {:keys [id modified source target]} webmention]
    {:hash id ;; weepinbell's id is hash in sqlite
     :wm_source source
     :wm_target target
     :wm_target_domain (get-domain target)
     :wm_source_domain (get-domain source)
     :wm_source_path (get-path source)
     :wm_target_path (get-path target)
     :lastmod (or modified (System/currentTimeMillis))}))

(defn mf->schema [wm_id data]
  (let [mf (:microformats data)
        target (get-in data [:webmention :target])]
    {:wm_id wm_id
     :mf_type (->> mf use-items find-h-entry (search-mftype target) name)
     ;; conver EDN to raw string
     :mf_data (pr-str mf)}))

(defn- row->wrapped [ds wm-row]
  (when wm-row
    (let [wm_id (:id wm-row)]
      (schema->map wm-row 
                   (first (sql/find-by-keys ds :microformats {:wm_id wm_id})) 
                   (first (sql/find-by-keys ds :proxied_photo {:wm_id wm_id}))))))

;; sql

(defn insert-webmention [ds data]
  (let [mf (:microformats data)
        photo (get-in data [:extra :proxied-photo])
        wm_result (sql/insert! ds :webmention (wm->schema data))
        wm_id (:id wm_result)]
    (when mf
      (sql/insert! ds :microformats (mf->schema wm_id data)))
    (when photo
      (sql/insert! ds :proxied_photo
                   {:wm_id wm_id
                    :url photo}))))

(defn delete-webmention [ds hashcode]
  ;; microformats and photo will be deleted by
  ;; cascade rule by SQL, no need to handle it
  (sql/delete! ds :webmention {:hash hashcode}))

(defn get-webmention-by-hash [ds hashcode]
  (first (sql/find-by-keys ds :webmention 
                           {:hash hashcode}
                           {:order-by [:lastmod :id]})))

(defn get-webmention-by-pair [ds source target]
  (first (sql/find-by-keys ds :webmention 
                           {:wm_source source
                            :wm_target target}
                           {:order-by [:lastmod :id]})))
(defn get-webmention
  ([ds hashcode]
   (some->> (get-webmention-by-hash ds hashcode)
            (row->wrapped ds)))
  ([ds source target]
   (some->> (get-webmention-by-pair ds source target)
            (row->wrapped ds))))

(defn get-webmentions-by-domain
  ([ds domain]
   (get-webmentions-by-domain ds domain nil))
  ([ds domain path]
   (let [keys (cond-> {:wm_target_domain domain}
                (some? path) (assoc :wm_target_path path))]
     (->> (sql/find-by-keys ds :webmention keys
                            {:order-by [:lastmod :id]})
          (map (partial row->wrapped ds))))))

(defn- update-related! [ds wm_id data]
  (let [mf (:microformats data)
        photo (get-in data [:extra :proxied-photo])]
    (sql/delete! ds :microformats {:wm_id wm_id})
    (when mf
      (sql/insert! ds :microformats (mf->schema wm_id data)))
    (sql/delete! ds :proxied_photo {:wm_id wm_id})
    (when photo
      (sql/insert! ds :proxied_photo {:wm_id wm_id :url photo}))))

(defn update-webmention
  ([ds hashcode new]
   (when-let [wm (get-webmention-by-hash ds hashcode)]
     (sql/update! ds :webmention (wm->schema new) {:id (:id wm)})
     (update-related! ds (:id wm) new)))
  ([ds source target new]
   (when-let [wm (get-webmention-by-pair ds source target)]
     (sql/update! ds :webmention (wm->schema new) {:id (:id wm)})
     (update-related! ds (:id wm) new))))

(defn make-db [db-file]
  (io/make-parents db-file)
  {:dbtype "h2" :dbname db-file})
(defn make-ds [db]
  (jdbc/get-datasource db))
(defn make-datasource [db-file]
  (jdbc/with-options (make-ds (make-db db-file))
                     {:builder-fn as-unqualified-lower-maps}))

(def init-sql-script (io/resource "sql/h2_init.sql"))
(defn create-tables [ds]
  (doseq [:let [statements (-> init-sql-script slurp (split #";"))]
          statement (remove blank? statements)]
    (jdbc/execute! ds [statement])))

(defn use-store [db-file]
  (let [ds (make-datasource db-file)]
    (create-tables ds)
    {:append (partial insert-webmention ds)
     :update (partial update-webmention ds)
     :delete (partial delete-webmention ds)
     :get    (partial get-webmention ds)
     :get-by-domain (partial get-webmentions-by-domain ds)}))
