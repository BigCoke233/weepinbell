 (ns weepinbell.metadata.html-head
   "HTML <head> parser that takes a hickory document, then tries 
    to gather information from basic <meta> properties, Open Graph
    protocol, Twitter <meta> tags and <link> with rel attribute,
    and finally compiles to a JF2 object.
    
    This namespace also contains some utility functions to manipulate
    a hickory document."
   (:require [weepinbell.metadata.jf2-spec]))

(defn get-child
  "Get a specific type of tag inside a hickory object."
  [tag hickory]
  (->> hickory :content (filter #(= (:tag %) tag)) first))

(defn get-meta
  "Gets <meta> tags from a hickory document. Returns a seq of maps.
   e.g. ({:name \"description\", :content \"A sentence.\"})"
  [hickory]
  (->> hickory :content
       (filter #(= (:tag %) :meta))
       (map #(get-in % [:attrs]))
       (filter #(and (or (:name %) (:property %))
                     (:content %)))))
(defn get-rels
  "Gets <link> tags with rel attribute from a hickory document.
   Returns a seq of maps, e.g. ({:rel ..., :href ...})"
  [hickory]
  (->> hickory :content
       (filter #(= (:tag %) :link))
       (map #(get-in % [:attrs]))
       (filter #(and (:rel %)
                     (:href %)))))
(defn attrs->map
  "Converts a seq of maps to a regular map, like this:
   ({:name \"a\", :content \"a\"} {:property \"b\", :content \"b\"})
   becomes {\"a\" \"a\", \"b\" \"b\"}"
  [meta-attrs]
  (let [{:keys [property name content rel href]} meta-attrs]
    {(or name property rel) (or content href)}))

(defn get-metadata
  "Takes a hickory data object, returns a map of <meta> name/content"
  [hickory]
  (->> hickory (get-child :html) (get-child :head)
       get-meta (map attrs->map) (reduce merge)))

(defn get-title
  "Takes a hickory data object, returns content of <title>."
  [hickory]
  (->> hickory (get-child :html) (get-child :head)
       :content (filter #(= (:tag %) :title)) first
       :content first))

(defn get-reldata
  "Takes a hickory data object, returns a map of <link> rel/hrefs"
  [hickory]
  (->> hickory (get-child :html) (get-child :head)
       get-rels (map attrs->map) (reduce merge)))

(defn parse-meta
  "Takes hickory document, parses <meta> <link> <title> tags, 
   returns a jf2 map. Supports Open Graph protocol and more."
  [hickory]
  (let [data (get-metadata hickory)
        rels (get-reldata hickory)]
    {:type "entry"
     :name (or (get data "og:title")
               (get data "twitter:title")
               (get-title hickory))
     :url (or (get data "og:url")
              (get data "twitter:url")
              (get rels "canonical"))
     :author {:type "card"
              :name (or (get data "article:author")
                        (get data "twitter:creator")
                        (get data "profile:username")
                        (let [fullname
                              (str (get data "profile:first_name")
                                   " " (get data "profile:last_name"))]
                          (when (not= fullname " ") fullname)))
              :url nil
              :photo (get rels "icon")}
     :published (get data "article:published_time")
     :content ""
     :wm-property :mention
     :summary (or (get data "og:description")
                  (get data "description")
                  (get data "twitter:description"))}))


