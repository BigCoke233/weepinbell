(ns weepinbell.webhook
  (:require [org.httpkit.client :as hk]
            [taoensso.timbre :as timbre]
            [weepinbell.utils.url :refer [valid-http-url?]]))

(defn trigger
  "Send requests to a list of webhooks."
  ([urls]
   (trigger urls nil))
  ([keyw webhooks]
   (when-let [hooks (if (nil? webhooks) keyw
                         ;; if webhook list provided
                         ;; retrieve from config
                        (->> (get webhooks keyw)
                             (filter string?)
                             (filter valid-http-url?)))]
     (->> hooks
          (map #(@(hk/post %)
                 (timbre/info "Webhook triggered: " %)))
          doall))))

