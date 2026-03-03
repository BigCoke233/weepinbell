(ns weepinbell.processor
  (:require [clojure.core.async :refer [go-loop <!]]))


(defn process-webmention
  "Retrieve webmention from queue and process it."
  [wmc]
  (println "Processor started.")
  (go-loop []
           (let [wm (<! wmc)]
             (if wm
               (println "webmention found in queue")
               (println "Idling")))
           (recur)))

