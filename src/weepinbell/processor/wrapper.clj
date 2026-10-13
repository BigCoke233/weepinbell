(ns weepinbell.processor.wrapper
  "Wrapper component wraps a qualified webmention while processing, used by processor.
  It adds microformats and extra data and return complete webmention map ready for storage."
  (:require [clojure.java.io :as io]
            [weepinbell.http.downloader :refer [download-file]]
            [weepinbell.utils.url :refer [valid-http-url? get-base relative? normalize]]
            [weepinbell.microformats.searcher :refer [use-items search-photo find-h-entry find-h-card]]
            [mf2clj.core :as mf-parser]
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
  (or (->> mf use-items find-h-entry search-photo)
      (->> mf use-items find-h-card search-photo)))

(defn- proxy-mf-photo [photo-base raw-photo]
  (if raw-photo
    (let [photo-url (str raw-photo)
          file-ext (or (re-find #"\.(?:jpg|jpeg|png|gif|webp)" photo-url) ".jpg")
          filename  (str (digest photo-url) file-ext)
          local-path (str "data/avatars/" filename)
          public-path (str "/avatars/" filename)]
                 ;; download photo if not exists
      (when-not (.exists (io/file local-path))
        (timbre/info "Downloading microformats photo to proxy external image.")
        (download-file photo-url local-path))
      (str photo-base public-path))
    nil))

(defn complete-url [source-base url]
  (if (relative? url)
    (normalize (str source-base url))
    url))

(defn- wrap-webmention
  "Parse microformats and collect essential info. 
  Wrap source, target and collected data into a map and return it."
  [id source-html source target photo-base]
  (timbre/info "parsing microformats and wrapping up webmention" id)
  (let [mf (-> source-html mf-parser/parse :items)
        wm {:source source
            :target target
            :id (digest (str source target))
            :modified (System/currentTimeMillis)}
        proxied-photo (->> mf
                           find-mf-photo
                           (complete-url (get-base source))
                           (proxy-mf-photo photo-base))
        data {:webmention wm
              :microformats mf
              :extra {:proxied-photo proxied-photo}}]
    (when-not (s/valid? ::webmention-wrapped data)
      (timbre/warn "webmention" id "data corrupted before writing to storage"))
    data))

(defn wrap
  [data config]
  (when (:ok? data)
    (let [{:keys [source-html wm id]} data
          {:keys [s t]} wm]
      (-> data
          (assoc :new-data
                 (wrap-webmention id source-html s t (:base-url config)))
          (assoc :ok true)))))

