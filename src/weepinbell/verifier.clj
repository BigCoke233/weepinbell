(ns weepinbell.verifier
  "Verifies in-coming webmentions after receiver checks URL.
   This makes sure: 
   1) source contains target URL.
   2) source respond with 2** status code.
   3) source's Content-Type is either HTML or plain text."
  (:require [clojure.string :refer [includes? trim lower-case split]]
            [weepinbell.http.client :as client]
            [weepinbell.utils.digest :refer [digest-short]]
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
  ;; get first 1MB of source URL and verify
  (let [{:keys [status body headers error]} (client/get! source {:headers {"Range" "bytes=0-1048575"}})
        target-headers (:headers (client/head! target))
        id (digest-short (str source target))]
    (timbre/info "verifying webmention" id
                 (str "(" "source: " source ", target: " target ")"))
    (cond
      ;; handle error 
      error
      (do
        (timbre/error "error verifying webmention:" error)
        false)
      ;; verify HTTP status
      (or (not status) (< status 200) (>= status 300))
      (do
        (timbre/warn "invalid source of" id ", status:" status)
        false)
      ;; verify Content-Type 
      (or (not (verify-headers headers))
          (not (verify-headers target-headers)))
      (do
        (timbre/warn "source or target of" id "is neither HTML nor plain text content.")
        false)
      ;; verify inclusion of target URL
      ;; TODO needs to be more robust and performant
      (not (includes? (str body) target))
      (do
        (timbre/warn "invalid source of" id ": target not found in source body.")
        false)
      ;; true if all verified 
      :else true)))

