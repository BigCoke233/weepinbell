(ns weepinbell.wrapper
  (:require [weepinbell.utils :refer [javamap->map
                                      file-exists?
                                      download-file]]
            [weepinbell.config :refer [get-config]]
            [weepinbell.microformats :as microformats]
            [digest :as digest]
            [taoensso.timbre :as timbre]))

(defn- find-mf-photo [mf]
  (or (-> mf
          (get-in [:properties "author"])
          first
          (get-in ["properties" "photo"])
          first)
      (-> mf (get-in [:properties "photo"]) first)))

(defn- proxy-mf-photo [raw-photo]
  (if raw-photo
               (let [photo-url (str raw-photo)
                     file-ext (or (re-find #"\.(?:jpg|jpeg|png|gif|webp)" photo-url) ".jpg")
                     filename  (str (digest/md5 photo-url) file-ext)
                     local-path (str "data/avatars/" filename)
                     public-path (str "/avatars/" filename)]
                 ;; download photo if not exists
                 (when-not (file-exists? local-path)
                   (timbre/info "Downloading microformats photo to proxy external image.")
                   (download-file photo-url local-path))
                 (str (get-config :base-url) public-path))
                        nil))

(defn wrap
  "Parse microformats and collect essential info. Wrap source, target and collected data into a Clojure map and return it."
  [source target]
  (let [mf (-> source
               microformats/parse
               :items first
               javamap->map)
        wm {:source source
            :target target
            :id (digest/md5 (str source target))
            :modified (System/currentTimeMillis)}
        proxied-photo (-> mf find-mf-photo proxy-mf-photo)]
      {:webmention wm
       :microformats mf
       :extra {:proxied-photo proxied-photo}}))

