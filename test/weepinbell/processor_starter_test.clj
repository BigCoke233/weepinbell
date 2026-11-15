(ns weepinbell.processor-starter-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.core.async :refer [chan put! poll!]]
            [weepinbell.test-utility :refer [with-webmention]]
            [weepinbell.processor.facade :refer [start-processor]]
            [weepinbell.http.client]
            [weepinbell.processor.verifier]))

(def nop-callbacks
  {:on-receive (fn [_])
   :on-create (fn [_])
   :on-delete (fn [_])
   :on-update (fn [_])})
(def nop-storage
  {:get (fn [& _])
   :delete (fn [& _])
   :append (fn [& _])
   :update (fn [& _])
   :get-by-domain (fn [& _])})

(defn- test-processor
  ([test-wmc received?]
   (test-processor test-wmc received? 1 nil))
  ([test-wmc received? concurrency]
   (test-processor test-wmc received? concurrency nil))
  ([test-wmc received? concurrency on-receive]
   (let [pmeta {:wmc test-wmc
                :callbacks (assoc nop-callbacks :on-receive
                                  (or on-receive
                                      (fn [_] (put! received? true))))
                :storage nop-storage
                :config {:base-url "https://test.com"
                         :allowed-target-domains ["test.com"]
                         :pipeline-concurrency concurrency}}]
     (start-processor pmeta))))

(deftest start-processor-test
  (testing "processor starting and taking webmention"
    (let [test-wmc (chan), received? (chan)
          shutdown (test-processor test-wmc received?)]
      (put! test-wmc {:s "s" :t "t"})
      (Thread/sleep 50)
      (is (poll! received?))
      (shutdown)))
  (testing "graceful shutdown, should not take webmention after shutdown"
    (let [test-wmc (chan), received? (chan)
          shutdown (test-processor test-wmc received?)]
      (shutdown)
      (put! test-wmc {:s "s" :t "t"})
      (Thread/sleep 50)
      (is (nil? (poll! received?))))))

(deftest concurrent-processor-test
  (testing "processor processing all webmentions before shutting down"
    (let [test-wmc (chan), received? (chan)
          concurrency 3
          shutdown (test-processor test-wmc received? 3)]
      (is (fn? shutdown))
      (dotimes [_ concurrency] (put! test-wmc {:s "s" :t "t"}))
      (shutdown)
      (Thread/sleep 50)
      (dotimes [_ concurrency] (is (poll! received?)))))
  (testing "processor still works if Exceptions were thrown"
    (let [test-wmc (chan), received? (chan), fail-once (chan)]
      (put! fail-once true)
      (let [on-receive (fn [_]
                         ;; throw Exception at the first call
                         (if (poll! fail-once)
                           (throw (Exception.))
                           (put! received? true)))
            concurrency 5
            shutdown (test-processor test-wmc received? concurrency on-receive)]
        (dotimes [_ concurrency] (put! test-wmc {:s "s" :t "t"}))
        (Thread/sleep 50)
        (dotimes [_ (- concurrency 1)] (is (poll! received?)))
        (shutdown)))))

(deftest processor-integration-test
  (testing "A webmention request eventually reaches to the writer"
    (with-redefs [weepinbell.http.client/default-options {:timeout 1000}
                  weepinbell.processor.verifier/retry-times 0]
      (with-webmention :new
        ["/source" {:status 200
                    :headers {"Content-Type" "text/html"}
                    :body "http://localhost:38888/target"}
         "/target" {:status 200
                    :headers {"Content-Type" "text/plain"}}]
        (let [test-wmc (chan)
              appended (promise)
              pmeta {:wmc test-wmc
                     :callbacks nop-callbacks
                     :storage (-> nop-storage
                                  (assoc :get (fn [& _] nil))
                                  (assoc :append (fn [_] (deliver appended true))))
                     :config {:base-url "http://localhost:38888"
                              :allowed-target-domains ["localhost"]
                              :pipeline-concurrency 1}}
              stop (start-processor pmeta)]
          (put! test-wmc {:s "http://localhost:38888/source"
                          :t "http://localhost:38888/target"})
          (is (deref appended 2000 false))
          (stop))))))
