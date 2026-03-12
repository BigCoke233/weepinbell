(ns weepinbell.receiver
  "Receiver receives Webmention request (a source and a target); verify the request; return error message if any; push Webmention into queue of processor."
  (:require [clojure.string :refer [replace]]
            [clojure.core.async :refer [put!]]
            [weepinbell.utils :refer [validate-urls validate-target]]))

(defn verify-request
  "Receive source and target of webmention; performs a series of checks; return a list of error responses or nil values. nil means the check has passed. The caller needs to return the first errror response if any."
  [s t]
  (let [checks [;; The receiver MUST check that source and target are valid URLs [URL]
                ;; and are of schemes that are supported by the receiver.
                [(validate-urls s t) "Invalid URLs. Source and target have to be http(s) urls."]
                ;; The receiver MUST reject the request
                ;; if the source URL is the same as the target URL.
                [(not (= s t)) "Source and target are identical."]
                [(validate-target t) "Domain of target is not allowed. This receiver does not receive webmention on its behalf."]]]
    (map (fn [[passed? msg]]
           (when-not passed?
             {:status 400
              :body msg}))
         checks)))

(defn receive
  "Receive webmention; call verify-request; if verification passed, push webmention into queue."
  [wm wmc]
  (let [{:keys [s t]} wm
        ;; remove anchor, #...
        source (some-> s (replace #"#.*$" ""))
        target (some-> t (replace #"#.*$" ""))
        verification (verify-request source target)]
    ;; Request verification
    (if-let [errror-response (some identity verification)]
      ;; respond if request verification failed
      errror-response
      ;; proceed if request is ok
      (do
        ;; The receiver SHOULD check that target is a valid
        ;; resource for which it can accept Webmentions.
        ;; This check SHOULD happen synchronously to reject
        ;; invalid Webmentions before more in-depth verification begins.
        ;;
        ;; Webmention verification SHOULD be handled asynchronously
        ;; to prevent DoS (Denial of Service) attacks.
        (put! wmc wm)
        {:status 202
         :headers {"Content-Type" "text/plain"}
         :body "Webmention accepted"}))))

