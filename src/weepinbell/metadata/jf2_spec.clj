(ns weepinbell.metadata.jf2-spec
  (:require [clojure.spec.alpha :as s]
            [weepinbell.utils.url :refer [valid-http-url?]]))

(s/def ::type string?)

(s/def ::name string?)

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
  (s/or :content-map (s/keys :req-un [::html ::text])
        :string string?))

(s/def ::wm-property
  #{:like-of :in-reply-to :repost-of :rsvp :mention
    "like-of" "in-reply-to" "repost-of" "rsvp" "mention"})

(s/def ::summary
  (s/or :has-summary string?
        :no-summary nil?))

(s/def ::like-of valid-http-url?)
(s/def ::in-reply-to valid-http-url?)
(s/def ::repost-of valid-http-url?)
(s/def ::rsvp valid-http-url?)

(s/def ::jf2
  (s/keys :req-un [::type ::name ::url ::author ::published ::content ::wm-property]
          :opt-un [::summary ::like-of ::in-reply-to ::repost-of ::rsvp]))

