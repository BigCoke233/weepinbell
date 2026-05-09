(ns weepinbell.microformats-wrapper
  (:require [weepinbell.utils :refer [javamap->map]]
            [weepinbell.storage :as store]))

(defn- find-item
  "Find specific type of items (like h-entry, h-card) in parsed microformats."
  [typ mf]
  (let [items (get mf "items")
        get-type #(first (get % "type"))]
    (filter #(= (get-type %) typ) items)))

(defn- get-prop
  "Use keyword get get a property value from a microformats item."
  [entry prop]
  (-> entry
      javamap->map
      :properties
      javamap->map
      (get prop)))

(defn- get-prop-first
  "Alias of (first (get-prop ...))"
  [entry prop]
  (first (get-prop entry prop)))

(defn- get-mf-type [mf]
  (first (or (:type mf) (get mf "type"))))

(defn- get-entry-type
  "Get type of an h-entry item in keyword. (:like-of, :repost-of, :in-reply-to)"
  [entry]
  ;; rsvp type is not supported yet
  (or
   (->> [:like-of :repost-of :in-reply-to]
        (filter #(get-prop entry %))
        first)
   :mention))

(defn- get-card-info
  "Get essential information of an h-card item and flatten it into a map."
  [card]
  (when (and card
             (= (get-mf-type card) "h-card"))
    {:name (get-prop-first card :name)
     :nickname (get-prop-first card :nickname)
     :photo (get-prop-first card :photo)
     :url (get-prop-first card :url)}))

(defn wrap-entry
  "Wrap parsed raw microformats (h-entry) data. If no h-entry found, return nil."
  [entry]
  ;; make sure it's h-entry
  (when (and entry
             (= (get-mf-type entry) "h-entry"))
    {:author (get-card-info (get-prop-first entry :author))
     :type (get-entry-type entry)
     :published (get-prop-first entry :published)
     :content (javamap->map (get-prop-first entry :content))}))

