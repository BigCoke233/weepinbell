(ns weepinbell.metadata.microformats
  "Microformats parser that converts a microformats map to a JF2 equivalent.
   This namespace also contains utilities for handling microformats."
  (:require [weepinbell.utils.url :refer [normalize]]
            [weepinbell.metadata.jf2-spec]))

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

(defn use-prop-all [property data]
  (cond
    (not (map? data))            nil
    (has-key? data :properties)  (get-key (get-key data :properties) property)
    (has-key? data property)     (get-key data property)
    :else                        nil))

(defn use-prop [property data]
  (first-value (use-prop-all property data)))

(defn find-type [mftype items]
  (concat (filter #(some #{mftype} (type-vector %)) items)
          (mapcat #(find-type mftype (:children %)) items)))
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

(defn- assoc-wm-property
  "Associate like-of, in-reply-to, repost-of and rsvp properties
   to J2F base map, and add an additional :wm-property key."
  [base h-entry]
  (let [data (select-keys (:properties h-entry)
                          [:like-of :repost-of :in-reply-to :rsvp])
        props (keys data)]
    (if (empty? props)
      (assoc base :wm-property :mention)
      (-> base (merge data) (assoc :wm-property (first props))))))

(defn- assoc-content
  "Associate content and sumary to J2F base map."
  [base h-entry]
  (let [content (use-prop :content h-entry)
        summary (use-prop :summary h-entry)
        get-value (fn [data]
                    ;; convert microformats "content.value"
                    ;; to JF2 "content.text", weirdly necessary
                    (if (and (map? data) (:value data))
                      (dissoc (assoc data :text (:value data)) :value)
                      data))]
    (cond-> base
      content (assoc :content (get-value content))
      summary (assoc :summary (get-value summary)))))

(defn- assoc-author
  "Associate author map to J2F base map."
  [base h-card photo]
  (let [author {:type "card"
                :name (use-prop :name h-card)
                :url (normalize (use-prop :url h-card))}
        author (if photo
                 (assoc author :photo photo)
                 author)]
    (assoc base :author author)))

(defn parse-microformats
  "Convert microformats h-entry to JF2 format/"
  [mf]
  {:pre [(coll? mf)]}
  (let [items (use-items mf)
        entry (find-h-entry items)
        card (find-h-card items)
        author (or (use-prop :author entry) card)
        photo (or (search-photo author)
                  (search-photo card))
        base {:type "entry"
              :name (use-prop :name entry)
              :url (let [raw-url (use-prop :url entry)]
                     (normalize
                      (if (map? raw-url) (:value raw-url) raw-url)))
              :published (use-prop :published entry)}]
    (-> base
        (assoc-author author photo)
        (assoc-content entry)
        (assoc-wm-property entry))))
