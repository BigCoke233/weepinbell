(ns weepinbell.utils.digest 
  (:import java.security.MessageDigest))

(defn md5-hash [^String s] 
  (let [algorithm (MessageDigest/getInstance "MD5") 
        raw-bytes (.digest algorithm (.getBytes s "UTF-8"))] 
    (apply str (map #(format "%02x" %) raw-bytes))))
