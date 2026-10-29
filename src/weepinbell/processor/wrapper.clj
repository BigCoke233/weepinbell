(ns weepinbell.processor.wrapper
  "Wrapper component wraps a qualified webmention while processing, part of the pipeline.
   It adds metadata and extra, and return complete webmention map ready for storage."
  (:require [clojure.java.io :as io]
            [weepinbell.http.downloader :refer [download-file]]
            [weepinbell.utils.url :refer [get-base relative? normalize]]
            [weepinbell.metadata.jf2 :as jf2]
            [weepinbell.metadata.jf2-spec]
            [taoensso.timbre :as timbre]
            [weepinbell.utils.digest :refer [digest]]
            [clojure.string :refer [split]]))

(defn- proxy-photo
  "Download a photo to /data/avatars to serve from our server."
  [photo-base raw-photo]
  (let [photo-url (str raw-photo)
          ;; TODO might need to use MIME-type to get ext
        file-ext (str "." (last (split photo-url #"\.")))
        filename  (str (digest photo-url) file-ext)
        local-path (str "data/avatars/" filename)
        public-path (str "/avatars/" filename)]
                 ;; download photo if not exists
    (when-not (.exists (io/file local-path))
      (timbre/info "Downloading author photo to serve locally")
      (download-file photo-url local-path))
    (str photo-base public-path)))

(defn- complete-url
  "Prepend base URL if a URL is relative."
  [source-base url]
  (if (relative? url)
    (normalize (str source-base url))
    url))

;; wrappers
(defn- wrap-webmention-basics
  "Wrap basic webmention data, :source :target :id and :modified.
   Will use current system time in milliseconds for :modified."
  [{:keys [s t]} id]
  {:webmention {:source s, :target t,
                :id (or id (digest (str s t))),
                :modified (System/currentTimeMillis)}})

(defn- wrap-jf2
  "Parses JF2 from source html, and associate :jf2 key to it."
  [data source-html]
  (assoc data :jf2 (-> source-html jf2/parse)))

(defn- wrap-extra
  "Wrap extra data, i.e. proxied photo."
  [data photo-base]
  (let [{:keys [webmention jf2]} data]
    (if-let [photo (get-in jf2 [:author :photo])]
      (assoc data :extra
             {:proxied-photo
              (->> photo
                   (complete-url (get-base (:source webmention)))
                   (proxy-photo photo-base))})
      data)))

(defn wrap
  "Part of the processor pipeline. Takes :source-html from last
   handler of the pipeline, parses data from it, wraps data
   to :new-data in the data map, and proceed with ok signal."
  [data config]
  (when (:ok? data)
    (let [{:keys [source-html wm id]} data
          new-data (-> wm (wrap-webmention-basics id)
                       (wrap-jf2 source-html)
                       (wrap-extra (:base-url config)))]
      (-> data
          (assoc :new-data new-data)
          (assoc :ok? true)))))

