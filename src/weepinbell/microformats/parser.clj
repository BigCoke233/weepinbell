;; DEPRECATED !!!

(ns weepinbell.microformats.parser
  "Deprecated. See issue #20
  https://codeberg.org/eltrac/weepinbell/issues/20"
  (:require [clojure.string :refer [includes? lower-case]]
            [taoensso.timbre :as timbre])
  (:import [com.kylewm.mf2j Mf2Parser]
           [java.net URI]))

(defn parse
  "Parse microformats from a URL."
  [url]
  (let [max-retries 3]
    (loop [attempt 1]
      (let [result (try
                     (let [parser (doto (Mf2Parser.)
                                    (.setIncludeAlternates true)
                                    (.setIncludeRelUrls true))]
                       {:ok true :data (.parse parser (URI. url))})
                     (catch Exception e
                       {:ok false :exception e}))]
        (if (:ok result)
          (:data result)
          (let [e (:exception result)
                msg (.getMessage e)]
            (if (and (<= attempt max-retries)
                     (or (instance? java.net.SocketTimeoutException e)
                         (includes? (lower-case (str msg)) "timeout")))
              (let [wait-time (* attempt 2000)]
                (timbre/warn (format "MF2 Parsing timeout for %s (attempt %d/%d). Retrying in %dms..."
                                     url attempt max-retries wait-time))
                (Thread/sleep wait-time)
                (recur (inc attempt)))
              (do
                (timbre/warn (format "MF2 Parsing failed for %s after %d attempts. Error: %s"
                                     url attempt (or msg "Unknown error")))
                nil))))))))
