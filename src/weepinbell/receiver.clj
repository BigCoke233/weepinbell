(ns weepinbell.receiver
  (:import [org.apache.commons.validator.routines UrlValidator]
           [java.net URI])
  (:require [weepinbell.config :refer [get-config]]))

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

#_(defn validate-source [s])

;; 3.2.1 Request Verfification
(defn verify [s t]
  (and
    ;; The receiver MUST check that source and target are valid URLs [URL]
    ;; and are of schemes that are supported by the receiver.
    (validate-urls s t)
    ;; The receiver MUST reject the request
    ;; if the source URL is the same as the target URL.
    (not (= s t))
    ;; The receiver SHOULD check that target is a valid resource for which it can accept Webmentions.
    ;; This check SHOULD happen synchronously to reject invalid Webmentions before more in-depth verification begins.
    (validate-target t)
    #_(validate-source s)))

(defn receive [wm]
  (let [{:keys [s t]} wm]
    (if (verify s t)
      "Valid Webmention"
      "Invalid Webmention")))

