(ns weepinbell.webhook
  (:require [clj-http.client :as client]
            [taoensso.timbre :as timbre]
            [weepinbell.config :refer [get-config]]))

(defn trigger [keyw]
  ;; keyw = on-receive...
  (when-let [hooks (get (get-config :webhook) keyw)]
    (doall (map #((client/post %)
                  (timbre/info "Webhook triggered: " %))
                hooks))))

