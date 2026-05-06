(ns weepinbell.verifier
  "Verifies in-coming webmentions after receiver checks URL.
   This makes sure: 
   1) source contains target URL.
   2) source respond with 2** status code.
   3) source's Content-Type is either HTML or plain text."
  (:require [weepinbell.utils :refer [get-content-type plain-or-html?]]
            [clojure.string :refer [includes?]]
            [clj-http.client :as client]
            [taoensso.timbre :as timbre]))

(defn verify-content-types [source target]
  (let [validate #(-> % get-content-type plain-or-html? boolean)]
    (and (validate source) (validate target))))

(defn verify-mention
  "Get source and follow redirects to check if target link is present."
  [source target]
  (try
    (timbre/info (str "Verifying mention from: " source))
    ;; get first 1MB of source URL and verify
    (let [{:keys [status body]} (client/get source {:throw false
                                                    :headers {"Range" "bytes=0-1048575"}})]
      ;; Verify status code
      (if (and (>= status 200) (< status 300))
        ;; Verify inclusion of target URL
        ;; TODO needs to be more robust
        (if (includes? (str body) target)
          true
          (do 
            (timbre/error "Target link not found in source body.") 
            false))
        (do 
          (timbre/error "Source returned status:" status) 
          false)))
    (catch Exception e
      (timbre/error "Error verifying mention:" (.getMessage e))
      false)))

