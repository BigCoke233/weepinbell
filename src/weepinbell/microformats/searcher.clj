(ns weepinbell.microformats.searcher
  "This namespace contains functions used for searching in a microformats JSON.
  Microformats are indeterministic and dynamic. A specific properties could be 
  in different places and many forms, thus will require extra effort finding it.")

;; glossary
;;
;; find: finding elements of a specific type in a vector or list 
;; search: searching for s specific key in a map
;; use: make sure the data node passed to it is indeed of a specific type, reduce it or return nil otherwise

(defn use-items [data]
  (cond
    (not (coll? data))  nil
    (empty? data)       nil
    (contains? data :items) (:items data)
    (sequential? data)  data
    :else               nil))

(defn use-prop [property data]
  (cond
    (not (map? data))             nil
    (contains? data :properties)  (first (get-in data [:properties property]))
    (get data property)           (first (get data property))
    :else                         nil))

(defn find-type [mftype items]
  (filter #(some #{mftype} (:type %)) items))
(def find-h-entries (partial find-type "h-entry"))
(def find-h-cards (partial find-type "h-card"))

(defn find-h-entry [items]
  (-> items find-h-entries first))
(defn find-h-card [items]
  (-> items find-h-cards first))

(defn search-photo [node]
  (if-let [photo (use-prop :photo node)]
    (if (map? photo) (:value photo) photo)
    (let [author-photo (when-let [card (use-prop :author node)]
                         (search-photo card))]
      (if author-photo author-photo
          (when-let [card (use-prop :org node)]
            (search-photo card))))))


