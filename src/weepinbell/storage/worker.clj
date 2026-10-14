(ns weepinbell.storage.worker
  "Storage worker is a background loop that handles write operations 
   to the storage, which makes write/delete/update thread-safe."
  (:require [clojure.core.async :refer [go-loop chan put! <! close!]]
            [taoensso.timbre :as timbre]))

(defn- start-worker
  "Asynchrnously start worker loop that takes from writer-chan 
   and writes to storage when operation is received.
   Returns a function that stops the worker gracefully."
  [storage writer-chan]
  (go-loop [operation (<! writer-chan)]
    (let [{:keys [op args]} operation]
      (if (not= op :stop)
        (do ;; perform operation and recur
          (try
            (apply (op storage) args)
            (catch Throwable e
              (timbre/error "Something unexpected occrus while writing to storage.")
              (timbre/error e)))
          (recur (<! writer-chan)))
        (do
          (close! writer-chan)
          (timbre/info "shutting down storage worker.")))))
  ;; return a function that stops the loop
  (fn [] (put! writer-chan {:op :stop})))

(defn- put-op!
  "Asynchronously put an operation map in a channel."
  [channel op args]
  (put! channel {:op op :args args}))

(defn start
  "Start worker. Wraps a base storage and returns a vector of 
  a stopper function and a asynchronous storage vtable."
  [storage]
  (let [writer-chan (chan)
        put-op (partial put-op! writer-chan)
        stop-worker (start-worker storage writer-chan)
        vtable {:append #(put-op :append [%])
                :update (fn [& args] (put-op :update args))
                :delete (fn [& args] (put-op :delete args))
                :get (:get storage)
                :get-by-domain (:get-by-domain storage)}]
    [stop-worker vtable]))
