(ns weepinbell.utils.url
  (:import [java.net URL URI MalformedURLException URISyntaxException IDN]
           [org.nibor.autolink LinkExtractor LinkType]
           [java.util EnumSet])
  (:require [clojure.string :as string]))

(defn valid-http-url? [url-string]
  (try
    (let [url (URL. url-string)
          uri (.toURI url)
          scheme (.getScheme uri)
          host (. IDN toASCII (-> url .getHost (string/replace "www." "")))]
      (and (string/includes? host ".") ;; do not accept localhost and other non-TLD names
           (or (= scheme "http") (= scheme "https"))))
    (catch ClassCastException _ false) ;; not string
    (catch MalformedURLException _ false)
    (catch URISyntaxException _ false)))

(defn get-domain [url]
  (when (some? url)
    (-> url URI/create .getHost)))

(defn get-path [url]
  (when (some? url)
    (-> url URI/create .getPath)))

(defn get-target [wm]
  (get-in wm [:webmention :target]))

(defn- normlaize-path [^String path*]
  (let [path (or path* "")
        deslashed (if (and (> (count path) 1) (.endsWith path "/"))
                    (subs path 0 (dec (count path))) 
                    path)]
   (str (.normalize (URI. deslashed)))))
(defn- normalize-query [^String query]
  (some-> query
    (string/split #"&")
    sort
    (->> (string/join "&"))))
(defn normalize
  "Normalize URL by removing trailing slashes; removing anchor;
   replacing . and ..; ordering query params."
  [^String url]
  (try 
    (let [base   (URI. url)
        scheme (.getScheme base)
        auth   (.getAuthority base)
        path   (normlaize-path (.getPath base))
        query  (normalize-query (.getQuery base))]
      (str scheme "://" auth path
        (when (seq query) (str "?" query))))
    ;; fall back to original string if syntax is invalid
    (catch URISyntaxException _ url)))

(defn url= [& urls]
  (apply = (map normalize urls)))

(defn extract 
  "Given plain text. Returns a vector of URLs that appears in it."
  [^String text]
  {:pre [(string? text)]
   :post [(partial every? string?)]}
  (let [extractor (-> (LinkExtractor/builder)
                      (.linkTypes (EnumSet/of LinkType/URL))
                      .build)
        indexes (->> (.extractLinks extractor text)
                     (map (fn [link] 
                            [(.getBeginIndex link)
                             (.getEndIndex link)])))
        urls (map #(subs text (first %) (last %)) indexes)]
    (vec urls)))

