(ns weepinbell.webserver.counter
  "Service returning count of webmention lists, parallel to retriever."
  (:require [weepinbell.webserver.retriever :as ret]))

(defn- reduce-to-count [resp]
  (assoc resp :body {:count (-> resp :body count str)}))

(defn by-url [url store opts]
  (reduce-to-count (ret/by-url url nil store opts)))

(defn by-domain [domain store opts]
  (reduce-to-count (ret/by-domain domain nil store opts)))
