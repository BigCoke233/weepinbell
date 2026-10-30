(ns weepinbell.utils.digest
  (:import java.security.MessageDigest)
  (:require [clojure.string :refer [upper-case]]))

(defn- java-digest [^String s algo]
  (let [algorithm (MessageDigest/getInstance algo)
        raw-bytes (.digest algorithm (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) raw-bytes))))

(defn digest
  ([s]
   (digest s :sha-256 32))
  ([s algo length]
   (->> (java-digest s (upper-case (name algo)))
        (take length)
        (apply str))))
