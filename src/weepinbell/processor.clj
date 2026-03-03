(ns weepinbell.processor
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.string :refer [includes?]]
            [clj-http.client :as client]))

(defn verify-mention
  "Get source and follow redirects to check if target link is present."
  [source target]
  ;; the client follows redirects automatically
  (let [{:keys [body]}  (client/get source)]
    (println "Verifying mention")
    ;; check if body contains source
    ;; TODO needs to be more robust
    (includes? body target)))

(defn process-webmention
  "Retrieve webmention from queue and process it."
  [wmc]
  (println "Processor started.")
  (go-loop []
           (let [wm (<! wmc)]
             (println "Webmention found in queue.")
             (if wm
               (let [{:keys [s t]} wm]
                 (if (verify-mention s t)
                   (println "Found mention")
                   (println "Invalid mention")))
               (println "Idling")))
           (recur)))

