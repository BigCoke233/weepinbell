(defproject weepinbell "v0.2.0"
  :description "A Webmention receiver written in Clojure."
  :url "https://codeberg.org/eltrac/weepinbell"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.11.4"]
                 [org.clojure/core.async "1.8.741"]
                 [compojure "1.7.0"]
                 [ring/ring-defaults "0.7.0"]
                 [ring/ring-json "0.5.1"]
                 [ring/ring-jetty-adapter "1.7.0"]
                 [cheshire "6.1.0"]
                 [commons-validator "1.10.1"]
                 [clj-http "3.13.1"]
                 [com.kylewm/mf2j "0.0.4"]
                 [com.taoensso/timbre "6.8.0"]
                 [digest "1.4.10"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler weepinbell.handler/app}
  :main ^:skip-aot weepinbell.core
  :profiles {:uberjar {:aot :all}}
  :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                       [ring/ring-mock "0.3.2"]]}
  :license {:name "MIT License"
            :url "https://mit-license.org/"
            :distribution :repo})
