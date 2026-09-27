(ns weepinbell.webhook
  "Makes hooks for sending webhook requests to certain endpoints on certain events."
  (:require [taoensso.timbre :as timbre]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [ring.util.codec :refer [url-encode]]
            [weepinbell.utils.url :refer [valid-http-url? get-domain]]
            [weepinbell.http.client :as client]
            [weepinbell.webhook-presets :refer [request-presets]]))

(s/def ::hooks-map 
  (s/keys :req-un [::on-create ::on-update ::on-delete ::on-receive]))

;; utilities
(defn- send-webhook [url opts]
  (if (valid-http-url? url)
    (client/post url opts
                 (fn async-callback [{:keys [status _ _ error]}]
                   (cond
                     error (timbre/error "error ocurred sending webhook to" url ":" error)
                     (>= status 500) (timbre/error "something's wrong with webhook server of" url)
                     (>= status 400) (timbre/error "webhook url is invalid:" url)
                     :else (timbre/info "request sent to" url))))
    (timbre/warn "webhook url not valid, skipped" url)))

(defn make-request [event-key data request-fn]
  (cond
      ;; use presets
    (keyword? request-fn)
    ((get request-presets request-fn) event-key data)
      ;; use custom fn
    (fn? request-fn)
    (request-fn event-key data)
    ;; 
    :else (timbre/warn "unknown request-fn type")))

;; NOTE this looks ugly but it gets the job done
;; will use an extensible function if we need to
(defn make-url [event-key data url]
  (let [{:keys [webmention extra]} data
        source (:source webmention)
        target (:target webmention)]
    (-> url
        (string/replace "{event}" (or (name event-key) ""))
        (string/replace "{target}" (or target ""))
        (string/replace "{source}" (or source ""))
        (string/replace "{target-encoded}" (if target (url-encode target) ""))
        (string/replace "{source-encoded}" (if source (url-encode source) ""))
        (string/replace "{source-domain}" (or (get-domain source) ""))
        (string/replace "{target-domain}" (or (get-domain target) ""))
        (string/replace "{photo}" (or (:proxied-photo extra) "")))))

(s/def ::event-action
  (s/keys :req-un [::url ::request-fn]))
(s/def ::event-action-candidate
  (s/or :string string?
        :map #(s/valid? ::event-action %)))
(defn make-actions [default-request-fn actions]
  (->> actions
       (filter #(s/valid? ::event-action-candidate %))
       (map #(if (map? %) %
                 {:url % :request-fn default-request-fn}))))

;; main function
(defn trigger
  "Given an event-key, a collection of actions (map of url and request-fn) and data,
  it transforms webhook URL with data, and constructs request body with request-fn.
  and sends POST request to transformed URL with constructed body."
  [event-key event-actions data]
  (->> event-actions
       (filter #(s/valid? ::event-action %))
       (map #(send-webhook (make-url event-key data (:url %))
                           (make-request event-key data (:request-fn %))))
       doall))

(def empty-hooks 
  (zipmap [:on-receive :on-create :on-delete :on-update]
          (repeat 4 (fn [& _]))))
(defn make-hooks
  "Given config (:webhook and :webhook-request-fn), it makes a vtable of hook functions
  that processor calls when certain events are triggered."
  [config]
  {:post [(s/valid? ::hooks-map %)]}
  (let [webhooks (:webhook config)
        event-keys (keys webhooks)
        default-request-fn (:webhook-request-fn config)]
    (->> webhooks
         (map (fn [webhook]
                (let [event-key (first webhook)
                      event-actions (make-actions
                                     default-request-fn (last webhook))]
                  (partial trigger event-key event-actions))))
         (zipmap event-keys)
         (merge empty-hooks))))

