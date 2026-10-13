#!/usr/bin/env bb
(ns treeshake)

(require '[babashka.tasks :refer [shell]])
(require '[babashka.fs :as fs])
(require '[cheshire.core :as json])

(defn treeshake [content original target]
  (let [shake (partial shell {:out :string}
                       "npx" "purgecss"
                       "--css" original
                       "--content")
        out (apply shake content)
        css (-> out :out json/parse-string first (get "css"))]
    (spit target css)))

(def original-css "resources/public/css/pico.min.css")
(def target-css "resources/public/css/pico.purged.css")
(def html-file "resources/public/index.html")
(def js-file "src/bellsprout/core.cljs")

(defn treeshake-pico []
  (treeshake [html-file js-file] original-css target-css)
  (if (fs/exists? target-css)
    (let [size #(.length (java.io.File. %))
          old-size (size original-css)
          new-size (size target-css)]
      (println "Tree shaken. Purged CSS file size:" new-size "Bytes"
               (str "(" (- new-size old-size) " Bytes)")))
    (println "Something went wrong.")))

(defn -main []
  (treeshake-pico))
