(ns weepinbell.utils
  (:require [weepinbell.http.client :as client]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]))

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
      (let [response (client/get! url {:as :stream})
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

(defn format-list [coll]
  (let [total (count coll)]
    (cond
      (= total 0) ""
      (= total 1) (first coll)
      :else (str (clojure.string/join ", " (butlast coll))
                 " and "
                 (last coll)))))

