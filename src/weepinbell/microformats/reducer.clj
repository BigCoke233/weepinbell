(ns weepinbell.microformats.reducer
  (:require [weepinbell.microformats.searcher :refer [use-items find-h-card find-h-entry 
                                                      search-photo use-prop]]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as timbre]))

(s/def ::author 
  (s/keys :req-un [::name]
          :opt-un [::url ::photo]))
(s/def ::reduced-microformats
  (s/keys :req-un [::author ::type ::published ::content]))

(defn find-type [h-entry]
  (or 
    (->> [:like-of :repost-of :in-reply-to]
         (filter #(use-prop % h-entry))
         first)
    :mention))

(defn reduce-mf [mf]
  (let [items (use-items mf)
        entry (find-h-entry items)
        card (find-h-card items)
        author (->> entry (use-prop :author))
        photo (or (search-photo author)
                  (search-photo card))
        data {:author {:name  (use-prop :name author) 
                       :url   (use-prop :url author)
                       :photo photo}
              :type (find-type entry)
              :published (use-prop :published entry)
              :content (use-prop :content entry)}]
    (when (not (s/valid? ::reduced-microformats data))
      (timbre/warn "Microformats weren't properly reduced: " mf))
    data))

