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

(defn validate [s t]
  (validate-urls s t)
  #_(validate-source s)
  #_(validate-target t))

(defn receive [wm]
  (let [{:keys [s t]} wm]
    (if (validate s t)
      "Valid Webmention"
      "Invalid Webmention")))

