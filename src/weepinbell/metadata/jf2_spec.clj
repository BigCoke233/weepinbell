(ns weepinbell.metadata.jf2-spec
  (:require [clojure.spec.alpha :as s]
            [weepinbell.utils.url :refer [valid-http-url?]]))

(s/def ::type string?)

(s/def ::name
  (s/or :missing-name nil?
        :has-name string?))

(s/def ::url
  (s/or :has-value valid-http-url?
        :lacks-value nil?))

(s/def ::published
  (s/or :date-string string?
        :timestamp int?
        :no-date nil?))

(s/def ::author
  (s/keys :req-un [::type ::name ::url]
          :opt-un [::photo]))

(s/def ::content
  (s/or :content-map (s/keys :req-un [::text]
                             :opt-un [::html])
        :string string?))

(s/def ::wm-property
  #{:like-of :in-reply-to :repost-of :rsvp :mention
    "like-of" "in-reply-to" "repost-of" "rsvp" "mention"})

(s/def ::summary
  (s/or :has-summary string?
        :no-summary nil?))

(s/def ::like-of 
  (s/and sequential? #(every? valid-http-url? %)))
(s/def ::in-reply-to 
  (s/and sequential? #(every? valid-http-url? %)))
(s/def ::repost-of 
  (s/and sequential? #(every? valid-http-url? %)))
(s/def ::rsvp 
  (s/or :single-answer string?
        :multi-answer #(every? string? %)))

(s/def ::jf2
  (s/keys :req-un [::type ::name ::url ::author ::published ::wm-property]
          :opt-un [::content ::summary ::like-of ::in-reply-to ::repost-of ::rsvp]))

