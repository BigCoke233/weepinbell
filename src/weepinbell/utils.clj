(ns weepinbell.utils
  (:import [org.apache.commons.validator.routines UrlValidator]
           [java.net URI])
  (:require [weepinbell.config :refer [get-config]]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json]))

(defn get-domain [url]
  (when (some? url)
    (-> url URI/create .getHost)))

(defn get-path [url]
  (when (some? url)
    (-> url URI/create .getPath)))

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
        (timbre/error status " error trying to get content type of " url)
        nil))
    (catch Exception e
        (timbre/error "Error trying to get content type of " url
                      (.getMessage e))
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

(defn javamap->map [map]
  (->> (seq map) (into {}) walk/keywordize-keys))

(defn download-file
  "Download file to destination. Return true if success, false if failed."
  [url dest-path]
  (try
    (let [file (io/file dest-path)
          parent-dir (.getParentFile file)]
      (when (and parent-dir (not (.exists parent-dir)))
        (.mkdirs parent-dir))
      (let [response (client/get url {:as :stream
                                     :conn-timeout 10000
                                     :socket-timeout 30000
                                     :throw-exceptions true})
            content-length (get-in response [:headers "content-length"])]
        (timbre/info "Downloading [" url "] -> [" dest-path "]")
        (when content-length
          (timbre/info "File size:" content-length "bytes"))
        ;; write to output stream
        (with-open [in (:body response)
                    out (io/output-stream file)]
          (io/copy in out))
        (timbre/info "Download success.")
        true))
    ;; when download failed
    (catch Exception e
      (timbre/error "Download error:" (.getMessage e))
      false)))

(defn file-exists? [path]
  (.exists (io/file path)))

(defn respond-json
  ([data]
    (respond-json data 200))
  ([data status]
  (let [json-string (json/generate-string data)
        status status]
    {:status status
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body json-string})))

(defn format-list [coll]
  (let [total (count coll)]
    (cond
      (= total 0) ""
      (= total 1) (first coll)
      :else (str (clojure.string/join ", " (butlast coll))
                 " and "
                 (last coll)))))

