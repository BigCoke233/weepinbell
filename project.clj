(defproject weepinbell "v0.6.3"
  :description "A Webmention receiver written in Clojure."
  :url "https://codeberg.org/eltrac/weepinbell"
  :license {:name "GNU Affero General Public License"
            :url "https://www.gnu.org/licenses/agpl-3.0.html"
            :distribution :repo}
  :min-lein-version "2.0.0"
  ;; deps
  :dependencies [;; when upgrading clojure, replace badge in README as well
                 [org.clojure/clojure "1.12.6"]
                 [org.clojure/core.async "1.9.865"]
                 [compojure "1.7.2"]
                 [ring/ring-core "1.15.5"]
                 [ring/ring-defaults "0.7.1"]
                 [http-kit "2.9.0-beta4"]
                 [ring/ring-json "0.5.1"]
                 [cheshire "6.2.0"]
                 [ac.eltr/mf2clj "0.0.3"]
                 [com.taoensso/timbre "6.8.0"]
                 [clj-rss "0.4.0"]]
  :managed-dependencies [[org.clojure/tools.reader "1.6.0"]
                         [org.ow2.asm/asm "9.8"]]
  :profiles {:uberjar {:aot :all}
             :test {:dependencies [[ring/ring-mock "0.6.2"]]}
             :cljsbuild {:dependencies [[org.clojure/clojurescript "1.11.132"]]}}
  :main ^:skip-aot weepinbell.core
  ;; plugins
  :plugins [[lein-ancient "0.6.15"]
            [lein-ring "0.12.6"]
            ;; latest commits of cloverage were "co-authored" by Claude
            ;; we'll keep it on this version for now
            [lein-cloverage "1.2.2"] 
            [dev.weavejester/lein-cljfmt "0.16.5"]
            ;; for frontend
            [lein-cljsbuild "1.1.8"]]
  :ring {:handler weepinbell.handler/app}
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src-fe"]
     :compiler
     {:main bellsprout.core
      :output-to "resources/public/js/main.js"
      :output-dir "resources/public/js/out"
      :asset-path "js/out"
      :optimizations :none
      :source-map true
      :target :browser}}
    :prod
    {:source-paths ["src-fe"]
     :compiler
     {:main bellsprout.core
      :output-to "resources/public/js/main.js"
      :output-dir "target/cljsbuild"
      :optimizations :advanced
      :pretty-print false}}}})

