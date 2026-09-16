(ns weepinbell.webhook-presets
  "This component defines preset functions that transform data to webhook requests."
  (:require [weepinbell.utils.url :refer [get-domain]]))

;; helpers
(defn- request-title-helper [hook source-name]
  (case hook
    :on-receive "A new webmention received, processing..."
    :on-create (str source-name " mentioned you!")
    :on-delete (str source-name " deleted their mention")
    :on-update (str source-name " updated their mention")
    "Something unexpected happened"))

(defn- request-data-helper [data]
  {:source (get-in data [:webmention :source])
   :target (get-in data [:webmention :target])
   :source-domain (get-domain (get-in data [:webmention :source]))
   :photo (get-in data [:extra :proxied-photo])})

;; presets
(defn- ntfy-request [hook data]
  (let [{:keys [source target source-domain photo]} (request-data-helper data)]
    {:body (str "from " source "\n" "to " target)
     :headers {"Title" (request-title-helper hook source-domain)
               "Click" source
               "Icon" photo}}))

(defn- bark-request [hook data]
  (let [{:keys [source target source-domain photo]} (request-data-helper data)]
    {:form-params {"title" (request-title-helper hook source-domain)
                   "body" (str "from " source "\n" "to " target)
                   "url" source
                   "icon" photo}}))

(def request-presets {:ntfy ntfy-request :bark bark-request})

