(ns weepinbell.verifier
  "Verifies in-coming webmentions after receiver checks URL.
   This makes sure: 
   1) source contains target URL.
   2) source respond with 2** status code.
   3) source's Content-Type is either HTML or plain text."
  (:require [clojure.string :refer [includes? trim lower-case split]]
            [org.httpkit.client :as hk]
            [taoensso.timbre :as timbre]))

(defn- verify-headers [headers]
  (when-let [content-type (:content-type headers)]
    (-> content-type
        (split #";")
        first
        trim
        lower-case
        (#{"text/html" "text/plain"}))))

(defn verify-mention
  "Get source and follow redirects to check if target link is present."
  [source target]
  (try
    (timbre/info (str "Verifying mention from: " source))
    ;; get first 1MB of source URL and verify
    (let [{:keys [status body headers]} @(hk/get source {:headers {"Range" "bytes=0-1048575"}})
          target-headers (:headers @(hk/head target))]
      ;; Verify status code
      (if (and status (>= status 200) (< status 300))
        ;; Verify Content-Types
        (if (and (verify-headers headers)
                 (verify-headers target-headers))
          ;; Verify inclusion of target URL
          ;; TODO needs to be more robust
          (if (includes? (str body) target)
            true
            (do
              (timbre/error "Target link not found in source body.")
              false))
          (do
            (timbre/error "Source or target is neither HTML nor plain text content.")
            false))
        (do
          (timbre/error "Source returned status:" status)
          false)))
    (catch Exception e
      (timbre/error "Error verifying mention:" (.getMessage e))
      false)))

