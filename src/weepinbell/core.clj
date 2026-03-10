(ns weepinbell.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [weepinbell.handler :refer [app]])
  (:gen-class)) ;; Essential for "lein uberjar"

(defn -main [& args]
  ;; Check for a PORT environment variable, default to 3000
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (println "Starting Weepinbell server on port" port)
    (run-jetty app {:port port :join? true})))

