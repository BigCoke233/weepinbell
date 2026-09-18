(ns weepinbell.storage.spec
  "Spec of storage middle layer, which is a vtable/map of storage functions.
  The spec makes sure any map is a valid storage vtable."
  (:require [clojure.spec.alpha :as s]))

(s/def ::vtable
  (s/keys :req-un [::append ::update ::delete ::get ::get-by-domain]))

;; TODO Needs to define specs for ::append ::update and other keys.
;; make sure they're functions and has the right paramters.
