(ns weepinbell.processor
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.string :refer [includes?]]
            [clj-http.client :as client]
            [weepinbell.storage :as storage]
            [java-time.api :as jt]))

(defn verify-mention
  "Get source and follow redirects to check if target link is present."
  [source target]
  ;; the client follows redirects automatically
  (let [{:keys [body]}  (client/get source)]
    (println "Verifying mention")
    ;; check if body contains source
    ;; TODO needs to be more robust
    (includes? body target)))

(defn wrap-webmention
  "Make GET request to source and collect essential data. Wrap source, target and collected data into a Clojure map and return it."
  [source target]
  {:type "entry"
   :author {:type "card"
            :name nil
            :photo nil
            :url nil}
   :url source
   :published nil
   :content {:text nil
             :html nil}
   :wm-received (jt/local-date-time)
   :wm-source source
   :wm-target target
   :wm-id nil})

(defn process-verified-webmention
  "If a webmention is verified, fetch microformats data and store it."
  [source target]
  ;; log
  (println "\n🙋‍♂️ Valid webmention received:")
  (println "from " source)
  (println "to " target "\n")
  ;; wrap and store data
  (let [data (wrap-webmention source target)]
    (storage/append data)))

(defn process-webmention
  "Retrieve webmention from queue and process it."
  [wmc]
  (println "Processor started.")
  (go-loop []
    (when-let [wm (<! wmc)]
      (println "Webmention found in queue.")
      (let [{:keys [s t]} wm]
        (if (verify-mention s t)
          (process-verified-webmention s t)
          (println "Invalid mention")))
      (println "Idling"))
    (recur)))

