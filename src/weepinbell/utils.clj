(ns weepinbell.utils
  (:import [org.apache.commons.validator.routines UrlValidator]
           [java.net URI])
  (:require [weepinbell.config :refer [get-config]]))

(defn get-domain [url]
  (-> (URI/create url) .getHost))

(defn get-target [wm]
  (get-in wm [:webmention :target]))

(defn valid-http-url? [url]
  (let [schemes (into-array String ["http" "https"])
        validator (UrlValidator. schemes)]
    (.isValid validator url)))

(defn validate-urls [s t]
  (and (valid-http-url? s) (valid-http-url? t)))

(defn validate-target [t]
  (let [allow-list (get-config :allowed-target-domains)
        target-domain (get-domain t)]
    (some #{target-domain} allow-list)))

