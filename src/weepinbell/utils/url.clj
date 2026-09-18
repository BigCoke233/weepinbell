(ns weepinbell.utils.url
  (:import [java.net URL URI MalformedURLException URISyntaxException IDN])
  (:require [clojure.string :as string]))

(defn valid-http-url? [url-string]
  (try
    (let [url (URL. url-string)
          uri (.toURI url)
          scheme (.getScheme uri)
          host (. IDN toASCII (-> url .getHost (string/replace "www." "")))]
      (and (string/includes? host ".") ;; do not accept localhost and other non-TLD names
           (or (= scheme "http") (= scheme "https"))))
    (catch ClassCastException _ false) ;; not string
    (catch MalformedURLException _ false)
    (catch URISyntaxException _ false)))

(defn get-domain [url]
  (when (some? url)
    (-> url URI/create .getHost)))

(defn get-path [url]
  (when (some? url)
    (-> url URI/create .getPath)))

(defn get-target [wm]
  (get-in wm [:webmention :target]))

