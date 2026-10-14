(ns weepinbell.processor.facade
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [go-loop <! put! close! chan <!!]]
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

(defn exec-pipeline [callbacks storage config wm]
  (let [{:keys [s t]} wm
        id (digest (str s t))]
    (try
      ((:on-receive callbacks) {:webmention {:id id :source s :target t}})
      (-> {:id id
           :wm wm
           :ok? true}
          (check-storage storage)
          verify
          (clean storage callbacks)
          (wrap config)
          (write storage callbacks))
      (catch Exception e
        (timbre/error "unexpceted exception in pipeline: " (.getMessage e))
        (timbre/debug e)))))

(defn start-processor
  [metadata]
  {:pre [(s/valid? ::processor-meta metadata)]
   :post [fn?]}
  (let [{:keys [wmc callbacks storage config]} metadata
        wmc-closed (chan)
        close-wmc (fn []
                    (close! wmc)
                    (close! wmc-closed))]
    ;; start processing loop
    (go-loop [wm (<! wmc)]
      (let [{:keys [stop?]} wm]
        (if stop?
          (do
            (close-wmc)
            (timbre/warn "webmention queue is cleared. processor shuts down."))
          ;; process found webmention
          (do
            (timbre/info "webmention found in queue, processing.")
            (exec-pipeline callbacks storage config wm)
            (recur (<! wmc))))))
    ;; return a function that stops the loop 
    (fn []
      (put! wmc {:stop? true})
      ;; after putting the stop signal, processor might not
      ;; stop immediately. wait for it to process what's left 
      ;; inside webmention queue and close it.
      (timbre/info "waiting for processor to shut down")
      (<!! wmc-closed))))
