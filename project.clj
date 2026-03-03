(defproject weepinbell "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.8.741"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.1"]
                 [cheshire "6.1.0"]
                 [commons-validator "1.4.1"]
                 [clj-http "3.13.1"]
                 [clojure.java-time "1.4.3"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler weepinbell.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
