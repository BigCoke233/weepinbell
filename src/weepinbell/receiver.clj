(ns weepinbell.receiver
  "Receiver receives Webmention request (a source and a target); verify the request; return error message if any; push Webmention into queue of processor."
  (:require [clojure.string :as string]
            [clojure.core.async :refer [put!]]
            [weepinbell.utils :refer [validate-urls validate-target]]
            [weepinbell.config :refer [get-config]]
            [digest :as digest]
            [cheshire.core :as json]))

(defn verify-request
  [source target]
  (when-let [error-msg (cond
                         ;; The receiver MUST check that source and target are valid URLs [URL]
                         ;; and are of schemes that are supported by the receiver.
                         (not (validate-urls source target)) "Invalid URLs. Source and target have to be valid http(s) urls."
                         ;; The receiver MUST reject the request
                         ;; if the source URL is the same as the target URL.
                         (= source target) "Source and target are identical."
                         (not (validate-target target)) "Domain of target is not allowed. This receiver does not receive webmention on its behalf.")]
    {:status 400
     :body error-msg}))

(defn receive
  "Receive webmention; call verify-request; if verification passed, push webmention into queue."
  [wm wmc]
  (let [{:keys [s t]} wm
        ;; remove anchor, #...
        source (some-> s (string/replace #"#.*$" ""))
        target (some-> t (string/replace #"#.*$" ""))]
    ;; Request verification
    (if-let [errror-response (verify-request source target)]
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
        ;; respond with 201 when Webmention passed checks and
        ;; was sent into queue, include a Location header
        (let [wm-id (digest/md5 (str source target))
              wm-url (str (get-config :base-url) "/api/mention/" wm-id)
              response {:status 201
                        :message "Webmention is accepted and pending for verification."
                        :url wm-url}]
          {:status 201
           :headers {"Location" wm-url}
           :body (json/generate-string response)})))))


