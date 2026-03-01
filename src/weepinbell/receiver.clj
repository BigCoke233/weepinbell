(ns weepinbell.receiver)

(import 'org.apache.commons.validator.UrlValidator)

(defn valid-http-url? [url]
  (let [schemes (into-array String ["http" "https"])
        validator (UrlValidator. schemes)]
    (.isValid validator url)))
(defn validate-urls [s t]
  (and (valid-http-url? s) (valid-http-url? t)))

#_(defn validate-source [s])

#_(defn validate-target [t])

;; 3.2.1 Request Verfification
(defn verify [s t]
  (and
    ;; The receiver MUST check that source and target are valid URLs [URL]
    ;; and are of schemes that are supported by the receiver.
    (validate-urls s t)
    ;; The receiver MUST reject the request
    ;; if the source URL is the same as the target URL.
    (not (= s t))
    #_(validate-source s)
    #_(validate-target t)))

(defn receive [wm]
  (let [{:keys [s t]} wm]
    (if (verify s t)
      "Valid Webmention"
      "Invalid Webmention")))

