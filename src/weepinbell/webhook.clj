(ns weepinbell.webhook
  (:require [clj-http.client :as client]
            [taoensso.timbre :as timbre]
            [weepinbell.config :refer [get-config]]
            [weepinbell.utils :refer [valid-http-url?]]))

(defn trigger [keyw]
  ;; keyw = on-receive...
  (when-let [hooks (get (get-config :webhook) keyw)]
    (->> hooks
         (filter string?)
         (filter valid-http-url?)
         (map #((client/post %)
                (timbre/info "Webhook triggered: " %)))
         doall)))

