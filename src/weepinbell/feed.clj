(ns weepinbell.feed
  (:require [clj-rss.core :as rss]
            [weepinbell.utils.url :refer [get-domain]]
            [weepinbell.utils.digest :refer [md5-hash]])
  (:import [java.time Instant]))

(defn make-feed-items [wms config]
  (->> wms
       (map #(let [wm (:webmention %)
                   id (if-let [id (:id wm)] id
                              (md5-hash (str (:source wm) (:target wm))))
                   permalink (str (:base-url config) "/api/mention/" id)
                   modified (:modified wm)
                   pub-date (when modified
                              (Instant/ofEpochMilli modified))]
               {:title (str (-> wm :source get-domain) " -> " (:target wm))
                :link (:source wm)
                :guid permalink
                :pubDate pub-date}))
       (take-last (:feed-length config))
       doall))

(defn make-feed-channel [title desc link wms config]
  (rss/channel-xml
   {:title title
    :link link
    :description desc}
   (when wms (make-feed-items wms config))))

(defn rss
  [domain storage config]
  (if ((set (:allowed-target-domains config)) domain)
    (let [wms ((:get-by-domain storage) domain)]
      {:headers {"Content-Type" "application/xml"}
       :body (make-feed-channel
              (str "Webmentions Feed of " domain)
              (str "Webmentions sent to " domain ", provided by Weepinbell.")
              (:base-url config)
              wms config)})
    {:status 404
     :headers {"Content-Type" "application/json"}
     :body {:message "No feed for this domain."}}))
