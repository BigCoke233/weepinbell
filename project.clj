(defproject weepinbell "v0.4.1"
  :description "A Webmention receiver written in Clojure."
  :url "https://codeberg.org/eltrac/weepinbell"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [org.clojure/core.async "1.8.741"]
                 [compojure "1.7.2"]
                 [ring/ring-core "1.15.4"]
                 [ring/ring-defaults "0.7.0"]
                 [http-kit "2.3.0"]
                 [ring/ring-json "0.5.1"]
                 [cheshire "6.1.0"]
                 [commons-validator "1.10.1"]
                 [com.kylewm/mf2j "0.0.4"]
                 [com.taoensso/timbre "6.8.0"]
                 [hiccup "2.0.0"]
                 [clj-rss "0.4.0"]
                 [ring/ring-mock "0.6.2"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-ring "0.12.6"]]
  :ring {:handler weepinbell.handler/app}
  :main ^:skip-aot weepinbell.core
  :profiles {:uberjar {:aot :all}}
  :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                       [ring/ring-mock "0.3.2"]]}
  :license {:name "GNU Affero General Public License"
            :url "https://www.gnu.org/licenses/agpl-3.0.html"
            :distribution :repo})
