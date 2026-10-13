(ns weepinbell.microformats.reducer
  (:require [weepinbell.microformats.searcher :refer [use-items find-h-card find-h-entry
                                                      search-photo use-prop search-mftype]]
            [clojure.spec.alpha :as s]))

(s/def ::author
  (s/keys :req-un [::name]
          :opt-un [::url ::photo]))
(s/def ::reduced-microformats
  (s/keys :req-un [::author ::type ::published ::content]))

(defn reduce-mf [target mf]
  (let [items (use-items mf)
        entry (find-h-entry items)
        card (find-h-card items)
        author (or (->> entry (use-prop :author))
                   card)
        photo (or (search-photo author)
                  (search-photo card))]
    {:author {:name  (use-prop :name author)
              :url   (use-prop :url author)
              :photo photo}
     :type (search-mftype target entry)
     :published (use-prop :published entry)
     :content (use-prop :content entry)}))

