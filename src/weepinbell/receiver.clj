(ns weepinbell.receiver
  "Receiver receives Webmention request (a source and a target); verify the request; return error message if any; push Webmention into queue of processor."
  (:require [clojure.string :as string]
            [clojure.core.async :refer [put!]]
            [weepinbell.utils :refer [valid-http-url? get-domain]]
            [md5.core :refer [string->md5-hex]]))

;; utilities
(defn- remove-anchor 
  "Takes a URL string. Removes anchor (#...)"
  [url]
  (string/replace url #"#.*" ""))
;; end utilities

;; validation and verification

(defn- validate-urls [s t]
  (and (valid-http-url? s) (valid-http-url? t)))

(defn- validate-target [t allow-list]
  (let [target-domain (get-domain t)]
    (some #{target-domain} allow-list)))

(defn- verify-request
  "Verify source and target based on URLs, return error message (string) if any. Return nil if none."
  [source target opts]
  (cond
    ;; The receiver MUST check that source and target are valid URLs [URL]
    ;; and are of schemes that are supported by the receiver.
    (not (validate-urls source target))
    "Invalid URLs. Source and target have to be valid http(s) urls."
    ;; The receiver MUST reject the request
    ;; if the source URL is the same as the target URL.
    (= source target)
    "Source and target are identical."
    ;; The receiver only accetps webmentions on behalf of configured domains.
    (not (validate-target target opts))
    "Domain of target is not allowed. This receiver does not receive webmention on its behalf."))

;; receiving logic

(defn receive
  "Receive webmention; call verify-request; if verification passed, push webmention into queue."
  [wm wmc config]
  (let [{:keys [s t]} wm
        source (remove-anchor s)
        target (remove-anchor t)
        new-wm {:s source :t target}
        allowed-domains (:allowed-target-domains config)]
     ;; Request verification
     (if-let [error-msg (verify-request source 
                                        target 
                                        allowed-domains)]
       ;; respond if request verification failed
       {:status 400
        :headers {"Content-Type" "application/json; charset=utf-8"}
        :body {:status 400
               :message error-msg}}
       ;; proceed if request is ok
       (do
         ;; The receiver SHOULD check that target is a valid
         ;; resource for which it can accept Webmentions.
         ;; This check SHOULD happen synchronously to reject
         ;; invalid Webmentions before more in-depth verification begins.
         ;;
         ;; Webmention verification SHOULD be handled asynchronously
         ;; to prevent DoS (Denial of Service) attacks.
         (put! wmc new-wm)
         ;; respond with 201 when Webmention passed checks and
         ;; was sent into queue, include a Location header
         (let [wm-id (string->md5-hex (str source target))
               wm-url (str (:base-url config) "/api/mention/" wm-id)
               response {:status 201
                         :message "Webmention is accepted and pending for verification."
                         :url wm-url}]
           {:status 201
            :headers {"Location" wm-url
                      "Content-Type" "application/json; charset=utf-8"}
            :body response})))))


