(ns weepinbell.storage.cache
  (:require [clojure.core.cache.wrapped :as w]
            [taoensso.timbre :as timbre]
            [weepinbell.utils.digest :refer [digest]]))

(defn- clear-cache! [cache]
  ;; w/lu-cache-factory returns a wrapped atom
  ;; while list-cache is already an atom
  ;; so deref the atom before putting it inside an atom
  (reset! cache @(w/lu-cache-factory {})))

(defn- evict-caches
  [caches id]
  (locking caches
    (w/evict (:ones caches) (keyword id))
    (clear-cache! (:list caches))))

(defn- update-caches
  [caches data]
  (locking caches
    (let [id (keyword (get-in data [:webmention :id]))
          cache (:ones caches)]
      (w/evict cache id)
      (w/through-cache cache id (constantly data)))
    (clear-cache! (:list caches))))

(defn cached-get
  ([cmeta source target]
   (if-let [wm (cached-get cmeta
                 (digest (str source target)))]
     wm 
     ((:get (:base cmeta)) source target)))
  ([{:keys [base caches]} id]
   (if-let [wm (w/lookup (:ones caches) (keyword id))]
     (do 
       (timbre/debug "Cache hit:" (get-in wm [:webmention :id]))
       wm)
     (when-let [wm ((:get base) id)]
       (w/through-cache (:ones caches)
         (keyword id) (constantly wm))
       wm))))

(defn cached-get-by-domain
  ([cmeta domain]
   (cached-get-by-domain cmeta domain nil))
  ([{:keys [base caches]} domain path]
   (if-let [wms (w/lookup (:list caches)
                          (keyword (digest (str domain path))))]
     (do
       (timbre/debug "Cache hit list:" domain path)
       wms)
     (when-let [wms ((:get-by-domain base) domain path)]
       (w/through-cache (:list caches)
         (keyword (digest (str domain path)))
         (constantly wms))
       wms))))

(defn delete-and-evict
  ([{:keys [base caches]} id]
   ((:delete base) id)
   (evict-caches caches id))
  ([{:keys [base caches]} source target]
   ((:delete base) source target)
   (evict-caches caches (digest (str source target)))))

(defn append-and-cache
  [{:keys [base caches]} data]
  ((:append base) data)
  (w/through-cache (:ones caches)
                   (keyword (get-in data [:webmention :id]))
                   (constantly data))
  (clear-cache! (:list caches)))

(defn update-and-update-cache
  [{:keys [base caches]} & args]
  (apply (:update base) args)
  (update-caches caches (last args)))

;; two cache buckets for two types of query
;;   ones-cache: get            (args: source, target; id)
;;   list-cache: get-by-domain  (args: domain, path)
(defn cache [base]
  (let [ones-cache (w/lu-cache-factory {})
        list-cache (w/lu-cache-factory {})
        cmeta {:base base, :caches {:list list-cache
                                    :ones ones-cache}}]
    {:get           (partial cached-get cmeta)
     :get-by-domain (partial cached-get-by-domain cmeta)
     :delete        (partial delete-and-evict cmeta)
     :append        (partial append-and-cache cmeta)
     :update        (partial update-and-update-cache cmeta)}))
