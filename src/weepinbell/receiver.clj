(ns weepinbell.receiver
  "Receiver receives Webmention request (a source and a target); verify the request; return error message if any; push Webmention into queue of processor."
  (:import [org.apache.commons.validator.routines UrlValidator]
           [java.net URI])
  (:require [weepinbell.config :refer [get-config]]
            [clojure.core.async :refer [go chan >!]]))

(defn valid-http-url? [url]
  (let [schemes (into-array String ["http" "https"])
        validator (UrlValidator. schemes)]
    (.isValid validator url)))
(defn validate-urls [s t]
  (and (valid-http-url? s) (valid-http-url? t)))

(defn validate-target [t]
  (let [allow-list (get-config :allowed-target-domains)
        target-domain (-> (URI/create t) .getHost)]
    (some #{target-domain} allow-list)))

(defn verify-request [s t]
  (let [checks [;; The receiver MUST check that source and target are valid URLs [URL]
                ;; and are of schemes that are supported by the receiver.
                (validate-urls s t) "Invalid URLs. Source and target have to be http(s) urls."
                ;; The receiver MUST reject the request
                ;; if the source URL is the same as the target URL.
                (not (= s t)) "Source and target are identical."
                (validate-target t) "Domain of target is not allowed"]]
    (for [[passed? msg] checks]
      (when-not passed?
        {:status 400
         :body msg}))))

(def wmc (chan)) ;; webmention queue

(defn receive [wm]
  (let [{:keys [s t]} wm]
    ;; Request verification
    (verify-request s t)
    ;; The receiver SHOULD check that target is a valid
    ;; resource for which it can accept Webmentions.
    ;; This check SHOULD happen synchronously to reject
    ;; invalid Webmentions before more in-depth verification begins.
    ;;
    ;; Webmention verification SHOULD be handled asynchronously
    ;; to prevent DoS (Denial of Service) attacks.
    (go (>! wmc wm))
    {:status 202
     :headers {"Content-Type" "text/plain"}
     :body "Webmention accepted"}))

