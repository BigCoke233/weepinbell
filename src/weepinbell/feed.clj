(ns weepinbell.feed
  (:require [clj-rss.core :as rss]
            [weepinbell.utils :refer [get-domain]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.storage :as storage]
            [digest :as digest])
  (:import [java.time Instant]))

(def feed-length (get-config :feed-length))

(defn rss
  [domain]
  (let [wms (storage/get-wms-by-domain domain)]
    {:headers {"Content-Type" "application/xml"}
     :body (rss/channel-xml
            {:title "Weepinbell Feed"
             :link (get-config :base-url)
             :description (str "Webmentions sent to " domain)}
            (->> wms
                 (map #(let [wm (:webmention %)
                             id (if-let [id (:id wm)] id
                                        (digest/md5 (str (:source wm) (:target wm))))
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
