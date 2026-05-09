(ns weepinbell.feed
  (:require [clj-rss.core :as rss]
            [weepinbell.utils :refer [get-domain]]
            [weepinbell.config :refer [get-config]]
            [md5.core :refer [string->md5-hex]])
  (:import [java.time Instant]))

(def feed-length (get-config :feed-length))

(defn make-feed-items [wms]
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
       doall))

(defn make-feed-channel [title desc wms]
  (rss/channel-xml
   {:title title
    :link (get-config :base-url)
    :description desc}
   (when wms (make-feed-items wms))))

(defn rss
  [domain storage]
  (if ((set (get-config :allowed-target-domains)) domain)
    (let [wms ((:get-by-domain storage) domain)]
      {:headers {"Content-Type" "application/xml"}
       :body (make-feed-channel
              (str "Webmentions Feed of " domain)
              (str "Webmentions sent to " domain ", provided by Weepinbell.")
              wms)})
    {:status 404
     :headers {"Content-Type" "application/json"}
     :body {:message "No feed for this domain."}}))
