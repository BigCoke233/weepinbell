(ns weepinbell.webserver.receiver
  "Service receiving Webmention request (a source and a target); verify the request; return error message if any; push Webmention into webbmention channel for processor to take."
  (:require [clojure.core.async :refer [put!]]
            [ring.util.http-response :refer [bad-request created]]
            [weepinbell.utils.url :refer [get-domain valid-http-url? normalize url=]]
            [weepinbell.utils.digest :refer [digest]]))

;; validation and verification

(defn- validate-urls [s t]
  (and (valid-http-url? s) (valid-http-url? t)))

(defn- validate-target [t allow-list]
  (let [target-domain (get-domain t)]
    (some #{target-domain} allow-list)))

(defn- check-request
  "Verify source and target based on URLs, return error message (string) if any. Return nil if none."
  [source target opts]
  (cond
    ;; The receiver MUST check that source and target are valid URLs [URL]
    ;; and are of schemes that are supported by the receiver.
    (not (validate-urls source target))
    "Invalid URLs. Source and target have to be valid http(s) urls."
    ;; The receiver MUST reject the request
    ;; if the source URL is the same as the target URL.
    (url= source target)
    "Source and target are identical."
    ;; The receiver only accetps webmentions on behalf of configured domains.
    (not (validate-target target opts))
    "Domain of target is not allowed. This receiver does not receive webmention on its behalf."))

;; receiving logic

(defn receive
  "Receive webmention; call verify-request; if verification passed, push webmention into queue.
  Return JSON response of pending notification, rejection or error."
  [wm wmc config]
  (let [{:keys [s t]} wm
        ;; if params are nil, use empty string 
        ;; empty strings will fail check-request
        source (if s (normalize s) "")
        target (if t (normalize t) "")
        new-wm {:s source :t target}
        allowlist (:allowed-target-domains config)]
     ;; Request verification
    (if-let [error-msg (check-request source target allowlist)]
       ;; respond if request verification failed
      (bad-request {:message error-msg
                    :source source
                    :target target})
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
        (let [wm-id (digest (str source target))
              wm-url (str (:base-url config) "/api/mention/" wm-id)
              response {:message "Webmention is accepted and pending for verification."
                        :source source
                        :target target
                        :url wm-url}]
          (created wm-url response))))))

(defn receive-batch
  "Receive multiple webmentions. Verify before putting into queue.
   If some webmentions are not unverified, return them, but the rest
   will be put into queue normally."
  [wms wmc config]
  (let [limit (:batch-receive-limit config)
        responses (pmap #(receive % wmc config)
                        (take limit wms))
        created (filter #(= 201 (:status %)) responses)
        invalid (filter #(= 400 (:status %)) responses)
        result (cond
                 (> (count wms) limit) [202 (str "Too many webmentions. Some were dropped. " 
                                                 "Only first " limit " were accepted.")]
                 (empty? created) [400 "All webmentions are invalid."]
                 (empty? invalid) [200 "All webmentions are received."]
                 :else [202 "Some webmentions are received. Some are invalid."])]
    {:status (first result)
     :body {:message (last result)
            :accepted created
            :rejected invalid}}))

