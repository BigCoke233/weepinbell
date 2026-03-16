(ns weepinbell.utils
  (:import [org.apache.commons.validator.routines UrlValidator]
           [java.net URI])
  (:require [weepinbell.config :refer [get-config]]
            [clj-http.client :as client]
            [clojure.string :as string]))

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

(defn get-content-type [url]
  (try
    (let [response (client/head url)]
      (get-in response [:headers "Content-Type"]))
    (catch clojure.lang.ExceptionInfo e
      (let [status (:status (ex-data e))]
        (println status " error trying to get content type of " url)
        nil))
    (catch Exception e
        (println "Error trying to get content type of " url)
        (println (.getMessage e))
      nil)))

(defn plain-or-html? [content-type]
  (cond
    (nil? content-type) false
    (string/includes? content-type "text/html") :html
    (string/includes? content-type "text/plain") :text
    :else false))

(defn validate-content-types [source target]
  (let [validate #(-> % get-content-type plain-or-html? boolean)
        source-valid (validate source)
        target-valid (validate target)]
    (and source-valid target-valid)))

