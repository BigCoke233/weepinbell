(defproject weepinbell "v1.0.0-4ab2ebf"
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
                 [metosin/ring-http-response "0.9.5"]
                 [http-kit "2.9.0-beta4"]
                 [ring/ring-json "0.5.1"]
                 [cheshire "6.2.0"]
                 [ac.eltr/mf2clj "0.0.5"]
                 [com.taoensso/timbre "6.8.0"]
                 [clj-rss "0.4.0"]
                 [org.nibor.autolink/autolink "0.12.0"]
                 [io.github.tonsky/fast-edn "1.2.0"]
                 [com.github.seancorfield/next.jdbc "1.3.828"]
                 [org.xerial/sqlite-jdbc "3.53.4.0"]
                 [nrepl "1.8.0"]]
  :managed-dependencies [[org.clojure/tools.reader "1.6.0"]
                         [org.ow2.asm/asm "9.11.1"]]
  :profiles {:uberjar {:aot :all
                       :uberjar-exclusions [#"^org/sqlite/native/Mac/"
                                            #"^org/sqlite/native/Windows/"
                                            #"^org/sqlite/native/Linux-Android/"]}
             :test {:dependencies [[ring/ring-mock "0.6.2"]]}
             :dev {:source-paths ["dev"]
                   :repl-options {:init-ns user
                                  :init (do (start) (watch))}
                   :dependencies [[io.github.tonsky/clj-reload "1.0.0"]
                                  [com.nextjournal/beholder "1.0.3"]]}}
  :aliases {"dev" ["with-profile" "+dev" "repl"]}
  :main ^:skip-aot weepinbell.core
  ;; plugins
  :plugins [[lein-ancient "0.6.15"]
            ;; latest commits of cloverage were "co-authored" by Claude
            ;; we'll keep it on this version for now
            [lein-cloverage "1.2.2"]
            [dev.weavejester/lein-cljfmt "0.16.5"]])

