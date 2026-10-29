(ns weepinbell.storage.spec
  "Spec of storage middle layer, which is a vtable/map of storage functions.
  The spec makes sure any map is a valid storage vtable."
  (:require [clojure.spec.alpha :as s]
            [weepinbell.utils.url :refer [valid-http-url?]]))

;; storage vtable

(s/def ::append fn?)
(s/def ::update fn?)
(s/def ::delete fn?)
(s/def ::get fn?)
(s/def ::get-by-domain fn?)

(s/def ::vtable
  (s/keys :req-un [::append ::update ::delete ::get ::get-by-domain]))

;; storage schema

(s/def ::source valid-http-url?)
(s/def ::target valid-http-url?)
(s/def ::id (s/and string?
                   #(let [len (count %)]
                      (or (= len 32)
                          (= len 8)))))
(s/def ::modified int?)
(s/def ::webmention
  (s/keys :req-un [::source ::target ::id ::modified]))

(s/def ::jf2
  #(s/valid? :weepinbell.metadata.jf2-spec/jf2 %))

(s/def ::proxied-photo valid-http-url?)
(s/def ::extra
  (s/keys :opt-un [::proxied-photo]))

(s/def ::webmention-wrapped
  (s/keys :req-un [::webmention]
          :opt-un [::jf2 ::extra]))

