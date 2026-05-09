(ns weepinbell.feed
  (:require [clj-rss.core :as rss]
            [weepinbell.utils :refer [get-domain]]
            [weepinbell.config :refer [get-config]]
            [md5.core :refer [string->md5-hex]])
  (:import [java.time Instant]))

(def feed-length (get-config :feed-length))

(defn rss
  [domain storage]
  (let [wms ((:get-by-domain storage) domain)]
    {:headers {"Content-Type" "application/xml"}
     :body (rss/channel-xml
            {:title "Weepinbell Feed"
             :link (get-config :base-url)
             :description (str "Webmentions sent to " domain)}
            (->> wms
                 (map #(let [wm (:webmention %)
                             id (if-let [id (:id wm)] id
                                        (string->md5-hex (str (:source wm) (:target wm))))
                             permalink (str (get-config :base-url) "/api/mention/" id)
                             modified (:modified wm)
                             pub-date (when modified
                                        (Instant/ofEpochMilli modified))]
                         {:title (str (-> wm :source get-domain) " -> " (:target wm))
                          :link (:source wm)
                          :guid permalink
                          :pubDate pub-date}))
                 (take-last feed-length)
                 doall))}))
