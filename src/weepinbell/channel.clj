;; channel.clj defines and exports 'wmc'
;; this is a channel (queue) in which
;; webmentions are put and retrieved
;;
;; it should only be referred to in core.clj
;; and handler.clj, and passed as an argument
;; to processer.clj and retriever.clj.
;; This keeps components loosely coupled.

(ns weepinbell.channel
  (:require [clojure.core.async :refer [chan]]))

(def wmc (chan))

