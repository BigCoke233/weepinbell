(ns weepinbell.processor.facade
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [go-loop <! put!]]
            [taoensso.timbre :as timbre]
            [weepinbell.utils.digest :refer [digest]]
            [weepinbell.storage.spec]
            ;; pipeline functions:
            [weepinbell.processor.storage-checker :refer [check-storage]]
            [weepinbell.processor.verifier :refer [verify]]
            [weepinbell.processor.janitor :refer [clean]]
            [weepinbell.processor.wrapper :refer [wrap]]
            [weepinbell.processor.writer :refer [write]]))

(s/def ::wmc (partial instance? clojure.core.async.impl.channels.ManyToManyChannel))
(s/def ::callbacks
  (s/keys :req-un [::on-receive ::on-create ::on-delete ::on-update]))
(s/def ::storage (partial s/valid? :weepinbell.storage.spec/vtable))
(s/def ::config
  (s/keys :req-un [::base-url ::allowed-target-domains]))
(s/def ::processor-meta
  (s/keys :req-un [::wmc ::callbacks ::storage ::config]))

(defn start-processor
  [metadata]
  {:pre [(s/valid? ::processor-meta metadata)]
   :post [fn?]}
  (let [{:keys [wmc callbacks storage config]} metadata]
    ;; start processing loop
    (go-loop [wm (<! wmc)]
      (let [{:keys [s t stop?]} wm
            id (digest (str s t))]
        (if stop?
          (timbre/warn "processor stopped on stop signal.")
          ;; process found webmention
          (do
            (timbre/info "webmention found in queue, processing.")
            ((:on-receive callbacks) {:webmention {:id id :source s :target t}})
            (-> {:id id
                 :wm wm
                 :ok? true}
                (check-storage storage)
                verify
                (clean storage callbacks)
                (wrap config)
                (write storage callbacks))
            (recur (<! wmc))))))
    ;; return a function that stops the loop 
    (fn [] (put! wmc {:stop? true}))))
