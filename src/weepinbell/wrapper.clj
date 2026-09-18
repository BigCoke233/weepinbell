(ns weepinbell.wrapper
  "Wrapper component wraps a qualified webmention while processing, used by processor.
  It adds microformats and extra data and return complete webmention map ready for storage."
  (:require [weepinbell.utils :refer [javamap->map
                                      file-exists?
                                      download-file]]
            [weepinbell.utils.url :refer [valid-http-url?]]
            [weepinbell.microformats.parser :as mf-parser]
            [taoensso.timbre :as timbre]
            [weepinbell.utils.digest :refer [digest]]
            [clojure.spec.alpha :as s]))

;;;; spec definitions
(s/def ::source valid-http-url?)
(s/def ::target valid-http-url?)
(s/def ::id (s/and string?
                   #(let [len (count %)]
                      (or (= len 32)
                          (= len 8)))))
(s/def ::modified int?)
(s/def ::webmention
  (s/keys :req-un [::source ::target ::id ::modified]))

;; NOTE current microformats parser is unstable
;; will have to postpone microformats spec definition
(s/def ::microformats map?)

(s/def ::proxied-photo valid-http-url?)
(s/def ::extra
  (s/keys :opt-un [::proxied-photo]))

(s/def ::webmention-wrapped
  (s/keys :req-un [::webmention]
          :opt-un [::microformats ::extra]))

;;;; utilities
(defn- find-mf-photo [mf]
  (or (-> mf
          (get-in [:properties "author"])
          first
          (get-in ["properties" "photo"])
          first)
      (-> mf (get-in [:properties "photo"]) first)))

(defn- proxy-mf-photo [raw-photo base-url]
  (if raw-photo
    (let [photo-url (str raw-photo)
          file-ext (or (re-find #"\.(?:jpg|jpeg|png|gif|webp)" photo-url) ".jpg")
          filename  (str (digest photo-url) file-ext)
          local-path (str "data/avatars/" filename)
          public-path (str "/avatars/" filename)]
                 ;; download photo if not exists
      (when-not (file-exists? local-path)
        (timbre/info "Downloading microformats photo to proxy external image.")
        (download-file photo-url local-path))
      (str base-url public-path))
    nil))

;;;; main function
(defn wrap
  "Parse microformats and collect essential info. 
  Wrap source, target and collected data into a map and return it."
  [source target base-url]
  (let [mf (-> source
               mf-parser/parse
               javamap->map
               :items first
               javamap->map)
        wm {:source source
            :target target
            :id (digest (str source target))
            :modified (System/currentTimeMillis)}
        proxied-photo (-> mf
                          find-mf-photo
                          (proxy-mf-photo base-url))
        data {:webmention wm
              :microformats mf
              :extra {:proxied-photo proxied-photo}}]
    (when-not (s/valid? ::webmention-wrapped data)
      (timbre/warn "webmention data corrupted before writing to storage" data))
    data))

