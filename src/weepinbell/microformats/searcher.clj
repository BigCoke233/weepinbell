(ns weepinbell.microformats.searcher
  "This namespace contains functions used for searching in a microformats JSON.
  Microformats are indeterministic and dynamic. A specific properties could be 
  in different places and many forms, thus will require extra effort finding it.")

;; glossary
;;
;; find: finding elements of a specific type in a vector or list 
;; search: searching for s specific key in a map
;; use: make sure the data node passed to it is indeed of a specific type, reduce it or return nil otherwise

(defn- get-key [data k]
  (cond
    (not (map? data))  nil
    (contains? data k) (get data k)
    (keyword? k)       (get data (name k))
    (string? k)        (get data (keyword k))
    :else              nil))

(defn- has-key? [data k]
  (cond
    (not (map? data))  false
    (contains? data k) true
    (keyword? k)       (contains? data (name k))
    (string? k)        (contains? data (keyword k))
    :else              false))

;; a property value is usually a vector; unwrap it but leave scalars and
;; unwrapped maps as they are.
(defn- first-value [value]
  (if (and (coll? value) (not (map? value)))
    (first value)
    value))

(defn- type-vector [item]
  (let [t (get-key item :type)]
    (cond
      (nil? t)     []
      (coll? t)    t
      (string? t)  [t]
      (keyword? t) [(name t)]
      :else        [t])))

(defn use-items [data]
  (cond
    (not (coll? data))      nil
    (empty? data)           nil
    (has-key? data :items)  (get-key data :items)
    (sequential? data)      data
    (has-key? data :type)   [data]
    :else                   nil))

(defn use-prop [property data]
  (cond
    (not (map? data))            nil
    (has-key? data :properties)  (first-value (get-key (get-key data :properties) property))
    (has-key? data property)     (first-value (get-key data property))
    :else                        nil))

(defn find-type [mftype items]
  (filter #(some #{mftype} (type-vector %)) items))
(def find-h-entries (partial find-type "h-entry"))
(def find-h-cards (partial find-type "h-card"))

(defn find-h-entry [items]
  (-> items find-h-entries first))
(defn find-h-card [items]
  (-> items find-h-cards first))

(defn search-photo [node]
  (if-let [photo (use-prop :photo node)]
    (if (map? photo) (get-key photo :value) photo)
    (let [author-photo (when-let [card (use-prop :author node)]
                         (search-photo card))]
      (if author-photo author-photo
          (when-let [card (use-prop :org node)]
            (search-photo card))))))
