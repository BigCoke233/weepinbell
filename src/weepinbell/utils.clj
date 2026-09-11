(ns weepinbell.utils
  (:import [org.apache.commons.validator.routines UrlValidator]
           [java.net URI])
  (:require [org.httpkit.client :as hk]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]))

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
      (let [response (hk/get url {:as :stream
                                  :timeout 30000})
            content-length (get-in @response [:headers "content-length"])]
        (timbre/info "Downloading [" url "] -> [" dest-path "]")
        (when content-length
          (timbre/info "File size:" content-length "bytes"))
        ;; write to output stream
        (with-open [in (:body @response)
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

(defn format-list [coll]
  (let [total (count coll)]
    (cond
      (= total 0) ""
      (= total 1) (first coll)
      :else (str (clojure.string/join ", " (butlast coll))
                 " and "
                 (last coll)))))

