(defproject weepinbell "v0.5.0"
  :description "A Webmention receiver written in Clojure."
  :url "https://codeberg.org/eltrac/weepinbell"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [org.clojure/core.async "1.8.741"]
                 [compojure "1.7.2"]
                 [ring/ring-core "1.15.4"]
                 [ring/ring-defaults "0.7.0"]
                 [http-kit "2.9.0-beta4"]
                 [ring/ring-json "0.5.1"]
                 [cheshire "6.1.0"]
                 [com.kylewm/mf2j "0.0.4"]
                 [com.taoensso/timbre "6.8.0"]
                 [hiccup "2.0.0"]
                 [clj-rss "0.4.0"]]
  :managed-dependencies [[org.clojure/tools.reader "1.5.2"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-ring "0.12.6"]
            [lein-cloverage "1.2.2"]
            [dev.weavejester/lein-cljfmt "0.16.5"]]
  :ring {:handler weepinbell.handler/app}
  :main ^:skip-aot weepinbell.core
  :profiles {:uberjar {:aot :all}
             :test {:dependencies [[ring/ring-mock "0.3.2"]]}}
  :license {:name "GNU Affero General Public License"
            :url "https://www.gnu.org/licenses/agpl-3.0.html"
            :distribution :repo})
