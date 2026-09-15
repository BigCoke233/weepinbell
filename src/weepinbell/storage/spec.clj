(ns weepinbell.storage.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::vtable
  (s/keys :req-un [::append ::update ::delete ::get ::get-by-domain]))
