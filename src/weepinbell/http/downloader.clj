(ns weepinbell.http.downloader
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [weepinbell.http.client :as client]))

(defn download-file
  "Download file to destination. Return true if success, false if failed."
  [url dest-path]
  (try
    (let [file (io/file dest-path)]
      (io/make-parents file)
      (let [response (client/get! url {:as :stream})
            status (:status response)]
        (if (and status (> status 199) (< status 300))
          (do
            (timbre/info "Downloading [" url "] -> [" dest-path "]")
            ;; write to output stream
            (with-open [in (:body response)
                        out (io/output-stream file)]
              (io/copy in out))
            (timbre/info "Download success. File size:" (.length file) "bytes")
            true)
          (do
            (timbre/warn "Download fail. Status: " status)
            (when-let [error (:error response)]
              (timbre/error error))
            false))))
    ;; when download failed
    (catch Throwable e
      (timbre/error "Download error:" (.getMessage e))
      (timbre/debug e)
      (when (.exists (io/file dest-path))
        (io/delete-file (io/file dest-path)))
      false)))

