(ns weepinbell.http.client
  "HTTP client adapter. Only this adapter depends directly on external HTTP client library.
  The rest of the codebase uses this adapter to make HTTP requests.
  All HTTP client related changeability and complexity is containend within here,
  for instance, all Exceptions are caught inside and returned as normal value."
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as hk]
            [taoensso.timbre :as timbre])
  (:import [org.httpkit.client TimeoutException]))

(def default-options
  {:user-agent "WebmentionBot"
   :timeout (* 20 1000)})

(defn- wrap-response [response]
  (assoc response :content-length (or (get-in response [:headers "Content-Length"])
                                      (get-in response [:headers "content-length"]))))

(defn- wrap-promise [promised-response]
  (future (wrap-response @promised-response)))

(defn post
  "Sends POST request asynchronously. Returns a promise/future."
  ([url]
   (post url nil))
  ([url options]
   (post url options nil))
  ([url options callbacks]
   (-> url
       (hk/post (merge default-options options) callbacks)
       wrap-promise)))

(defn get
  "Sends GET request asynchronously. Returns a promise/future."
  ([url]
   (get url nil))
  ([url options]
   (get url options nil))
  ([url options callbacks]
   (-> url
       (hk/get (merge default-options options) callbacks)
       wrap-promise)))

(defn get!
  "Sends GET request synchronously. Returns response."
  ([url]
   @(get url nil))
  ([url options]
   @(get url options nil))
  ([url options callbacks]
   @(get url options callbacks)))

(defn head
  "Sends HEAD request asynchronously. Returns a promise/future."
  ([url]
   (head url nil))
  ([url options]
   (head url options nil))
  ([url options callbacks]
   (-> url
       (hk/head (merge default-options options) callbacks)
       wrap-promise)))

(defn head!
  "Sends HEAD request synchronously. Returns a response."
  ([url]
   @(head url nil))
  ([url options]
   @(head url options nil))
  ([url options callbacks]
   @(head url options callbacks)))

(defn retryable? [error]
  (let [retryables #{TimeoutException}]
    (some #(instance? % error) retryables)))

(defmacro with-retry
  "Retry request after failure with backoff, asynchronously."
  [times backoff & body]
  `(future (loop [~'current-try 0
                  ~'backoff ~backoff]
             (let [~'result (try
                              (deref ~@body)
                              (catch Throwable ~'e {:error ~'e}))]
               (if (and (:error ~'result)
                        (retryable? (:error ~'result))
                        (< ~'current-try ~times))
                 (do
                   (timbre/warn "request failed. retry after" ~'backoff "seconds")
                   (Thread/sleep (* ~'backoff 1000))
                   (recur (inc ~'current-try) (* 2 ~'backoff)))
                 ~'result)))))

