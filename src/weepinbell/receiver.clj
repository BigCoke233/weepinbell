(ns weepinbell.receiver
  (:require [clojure.string :refer [starts-with?]]))

(defn validate-url [url]
  (or (starts-with? url "http://")
      (starts-with? url "https://")))
(defn validate-urls [s t]
  (and (validate-url s) (validate-url t)))

#_(defn validate-source [s])

#_(defn validate-target [t])

(defn validate [s t]
  (validate-urls s t)
  #_(validate-source s)
  #_(validate-target t))

(defn receive [wm]
  (let [{:keys [s t]} wm]
    (if (validate s t)
      "Valid Webmention"
      "Invalid Webmention")))

