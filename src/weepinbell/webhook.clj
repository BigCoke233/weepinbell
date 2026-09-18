(ns weepinbell.webhook
  "Makes hooks for sending webhook requests to certain endpoints on certain events.
  This component does not care about specific event types or endpoints.
  It takes them from its caller (core.clj) and makes a map of hook functions."
  (:require [taoensso.timbre :as timbre]
            [weepinbell.utils.url :refer [valid-http-url?]]
            [weepinbell.http.client :as client]))

(defn- send-webhook [url]
  (client/post url
               {:timeout 15000}
               (fn async-callback [{:keys [status _ _ error]}]
                 (cond
                   error (timbre/error "error ocurred sending webhook to" url ":" error)
                   (>= status 500) (timbre/error "something's wrong with webhook server of" url)
                   (>= status 400) (timbre/error "webhook url is invalid:" url)
                   :else (timbre/info "request sent to" url)))))

(defn trigger
  "Send requests to a list of webhooks.
  Given a vector/list of URLs, it sends plain POST request to each URL. 
  Given a keyword and a map of URL vectors/list, it sends request to each URL in (:keyword map)."
  ([urls]
   (trigger urls nil))
  ([keyw-or-urls webhooks]
   (when-let [hooks (if (nil? webhooks)
                      keyw-or-urls
                      (->> (get webhooks keyw-or-urls)
                           (filter string?)
                           (filter valid-http-url?)))]
     (when (keyword? keyw-or-urls)
       (timbre/info "Webhook" (name keyw-or-urls) "triggered."))
     (->> hooks (map send-webhook) doall))))

