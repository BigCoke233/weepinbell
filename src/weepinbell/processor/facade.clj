(ns weepinbell.processor.facade
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [go-loop <! put! chan <!!]]
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
(s/def ::base-url string?)
(s/def ::allowed-target-domains sequential?)
(s/def ::pipeline-concurrency int?)
(s/def ::config
  (s/keys :req-un [::base-url ::allowed-target-domains ::pipeline-concurrency]))
(s/def ::processor-meta
  (s/keys :req-un [::wmc ::callbacks ::storage ::config]))

(defn pipe
  "Takes variable number of functions and form a pipeline.
   Essentially a -> macro that returns a function, or a reversed comp."
  [& functions]
  (apply comp (reverse functions)))

(defn assemble-pipeline
  "Makes a proceesor pipeline. Returns a function that accepts webmention."
  [{:keys [callbacks storage config]}]
  (pipe
   ;; initializer function
   ;;; turns {:s ... :t ...} to pipeline map that the following functions
   ;;; pass around; calls :on-receive hook.
   (fn [wm]
     (let [{:keys [s t]} wm
           id (digest (str s t))]
       ((:on-receive callbacks) {:webmention {:id id :source s :target t}})
       {:id id
        :wm wm
        :ok? true}))
   ;; pipeline steps
   (partial check-storage storage)    ;; check if webmention exists
   verify                             ;; verify if target is mentioned
   (partial clean storage callbacks)  ;; delete old unverified webmention
   (partial wrap config)              ;; wrap metadata into webmention
   (partial write storage callbacks)));; write to storage

(defn exec-pipeline
  "Basically wraps (pipeline wm) call inside a try block.
   catches exceptions and errors so they don't paralyze pipelines."
  [pipeline wm]
  (try
    (pipeline wm)
    (catch Throwable e
      (timbre/error "unexpceted exception in pipeline: " (.getMessage e))
      (timbre/debug e))))

(defn dispatch-pipeline-loop
  "Start a loop that takes from wmc and do pipeline when data come in."
  [n wmc stopped pipeline]
  (go-loop [wm (<! wmc)]
    (let [{:keys [stop?]} wm]
      (if (or stop? (nil? wm))
        (do ;; handle stop signal
          (put! stopped true)
          (timbre/debug (str "#" n) "processor shuts down."))
        (do ;; process found webmention
          (timbre/info (str "#" n) "webmention found in queue, processing.")
          (exec-pipeline pipeline wm)
          (recur (<! wmc)))))))

(defn start-processor
  "Starts webmention processing pipelines. Returns a stopper function."
  [metadata]
  {:pre [(s/valid? ::processor-meta metadata)]
   :post [fn?]}
  ;; wmc is webmention queue; stopped is channel where processors 
  ;; put signals into, to notify that they're shut down
  (let [wmc (:wmc metadata), stopped (chan)
        concurrency (get-in metadata [:config :pipeline-concurrency])]
    ;; dispatch pipelines
    (doseq [n (range concurrency)
            :let [pipeline (assemble-pipeline metadata)]]
      (dispatch-pipeline-loop n wmc stopped pipeline))
    ;; return a function that stops the loop 
    (fn []
      (dotimes [_ concurrency] (put! wmc {:stop? true}))
      ;; after putting the stop signal, processor might not
      ;; stop immediately. wait for it to process what's left 
      ;; inside webmention queue and close it.
      (timbre/info "waiting for queue to clear, and processor to shut down")
      (dotimes [_ concurrency] (<!! stopped)))))
