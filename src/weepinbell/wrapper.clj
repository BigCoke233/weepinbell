(ns weepinbell.wrapper
  (:require [weepinbell.utils :refer [javamap->map
                                      file-exists?
                                      download-file]]
            [weepinbell.microformats-parser :as mf-parser]
            [taoensso.timbre :as timbre]
            [weepinbell.utils.digest :refer [digest]]))

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

(defn wrap
  "Parse microformats and collect essential info. Wrap source, target and collected data into a Clojure map and return it."
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
                          (proxy-mf-photo base-url))]
    {:webmention wm
     :microformats mf
     :extra {:proxied-photo proxied-photo}}))

