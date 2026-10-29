(ns weepinbell.storage-test
  (:require [clojure.test :refer [testing is deftest]]
            [weepinbell.storage.edn-storage :as edns]
            [weepinbell.storage.h2 :as h2]
            [weepinbell.storage.worker :as worker]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [weepinbell.storage.spec]))

;; preparing storages to test

(def test-edn-file "tmp/testfile")
(when (.exists (io/file test-edn-file))
  (io/delete-file test-edn-file))
(def test-h2-db "tmp/testh2")
(when (.exists (io/file (str test-h2-db ".mv.db")))
  (io/delete-file (str test-h2-db ".mv.db")))

(def edn-storage (edns/use-store test-edn-file))
(def h2-storage (h2/use-store test-h2-db))
(def storages [["bare-edn" edn-storage]
               ["bare-h2" h2-storage]
               ["worker-edn" (last (worker/start edn-storage))]
               ["worker-h2" (last (worker/start h2-storage))]])

;; check if storages conform spec

(def spec-check (partial s/valid? :weepinbell.storage.spec/vtable))
(deftest storage-conformity-test
  (testing "storage should conform storage spec."
    (with-redefs [weepinbell.storage.edn-storage/file-exists? (fn [_] true)]
      (doseq [storage storages]
        (is (spec-check (last storage))))))
  (testing "spec-check invalid storage vtables"
    (doseq [check [{:get-by-domain (fn [_])}
                   {:append (fn [_]) :get (fn [_])}
                   "I am storage. Trust me"
                   nil 1 [] {}]]
      (is (not (spec-check check))))))

;; basic general test suite

(defmacro wait-til! [condition-expr & body]
  `(let [~'start (System/currentTimeMillis)]
     (loop []
       (if (try ~condition-expr (catch Throwable ~'_ false))
         (do ~@body)
         (when (< (- (System/currentTimeMillis) ~'start) 10000)
           (Thread/sleep 10)
           (recur))))))

(defn- wm= [& wms]
  (->> (map :webmention wms)
       (map #(select-keys % [:target :source :modified]))
       (apply =)))

(defn- test-append-and-get [storage]
  (let [id "123456789"
        source "https://source.net"
        target "https://target.net"
        data {:webmention {:source source
                           :target target
                           :id id
                           :modified (System/currentTimeMillis)}}]
    ((:append storage) data)
    (wait-til! ((:get storage) id))
    (is (wm= data ((:get storage) id)))
    (is (wm= data ((:get storage) source target)))
    ((:delete storage) id)
    (wait-til! (nil? ((:get storage) id)))
    (is (nil? ((:get storage) id)))))

(defn- test-append-and-get-list [storage]
  (let [domain "target.com"
        data [{:webmention {:source "https://source.net"
                            :target (str "https://" domain "/a")
                            :id "35231"
                            :modified (System/currentTimeMillis)}}
              {:webmention {:source "https://source.net"
                            :target (str "https://" domain "/b")
                            :id "232425"
                            :modified (System/currentTimeMillis)}}]
        ids (map #(:id (:webmention %)) data)]
    (doseq [item data]
      ((:append storage) item))
    (wait-til! (->> data
                    (map (fn [item] (get-in item [:webmention :id])))
                    (every? #((:get storage) %))))
    (is (= (count data) (count ((:get-by-domain storage) domain))))
    (is (= 1 (count ((:get-by-domain storage) domain "/b"))))
    (doseq [id ids]
      ((:delete storage) id))
    (wait-til! (= 0 (count ((:get-by-domain storage) domain))))
    (is (= 0 (count ((:get-by-domain storage) domain))))))

(defn- test-update [storage]
  (let [id "987654321"
        source "https://source.com"
        target "https://target.com"
        data {:webmention {:id id
                           :source source
                           :target target
                           :modified (System/currentTimeMillis)}}
        data-wm (:webmention data)
        data-new (assoc data :webmention
                        (assoc data-wm :modified
                               (+ (System/currentTimeMillis) 1)))
        data-neo (assoc data-new :webmention
                        (assoc data-wm :modified
                               (+ (System/currentTimeMillis) 2)))]
    ((:append storage) data)
    (wait-til! ((:get storage) id))
    ((:update storage) id data-new)
    (wait-til! (wm= data-new ((:get storage) id)))
    (is (wm= data-new ((:get storage) id)))
    ((:update storage) source target data-neo)
    (wait-til! (wm= data-neo ((:get storage) source target)))
    (is (wm= data-neo ((:get storage) source target)))
    ((:delete storage) id)))

(defn- test-get-empty [storage]
  (is (nil? ((:get storage) nil "target")))
  (is (nil? ((:get storage) "source" nil)))
  (is (nil? ((:get storage) nil nil))))

(defn- test-friends [storage]
  (let [id "123456789", mf {}
        photo "https://source.net/photo.png"
        data {:webmention {:id id
                           :source "https://source.net"
                           :target "https://target.net"
                           :modified (System/currentTimeMillis)}
              :jf2 mf
              :extra {:proxied-photo photo}}]
    ((:append storage) data)
    (wait-til! ((:get storage) id))
    (let [retrieved ((:get storage) id)]
      (is (= photo (get-in retrieved [:extra :proxied-photo])))
      (is (= mf (:jf2 retrieved))))
    ((:delete storage) id)
    (wait-til! (not ((:get storage) id)))))

(defn- test-result-order [storage]
  (let [domain "target.com"
        data [{:webmention {:source "https://source.net"
                            :target (str "https://" domain "/a")
                            :id "135234"
                            :modified (System/currentTimeMillis)}}
              {:webmention {:source "https://source.net"
                            :target (str "https://" domain "/b")
                            :id "24645"
                            :modified (+ (System/currentTimeMillis) 1)}}
              {:webmention {:source "https://source.net"
                            :target (str "https://" domain "/b")
                            :id "334324"
                            :modified (+ (System/currentTimeMillis) 2)}}
              {:webmention {:source "https://source.net"
                            :target (str "https://" domain "/b")
                            :id "45343"
                            :modified (+ (System/currentTimeMillis) 3)}}]
        ids (map #(:id (:webmention %)) data)]
    (doseq [item (shuffle data)]
      ((:append storage) item))
    (wait-til! (every? #((:get storage) (:id (:webmention %))) data))
    (let [result ((:get-by-domain storage) domain)]
      (is (= (mapv #(get-in % [:webmention :id]) result)
             (mapv #(get-in % [:webmention :id]) data))))
    (doseq [id ids] ((:delete storage) id))
    (wait-til! (every? #(not ((:get storage) (:id (:webmention %)))) data))))

(deftest general-storage-test
  (doseq [storage storages]
    (doseq [testcase [[test-append-and-get "appending webmention"]
                      [test-append-and-get-list "appending and getting by domain"]
                      [test-update "updating webmentions"]
                      [test-get-empty "making sure getting empty won't throw"]
                      [test-friends "friends of webmention (microformats, proxied-photo) can be stored correctly"]
                      [test-result-order "result should be in chronological order"]]]
      (testing (str (first storage) ": " (last testcase))
        ((first testcase) (last storage))))))

;; storage worker specifics

(deftest storage-worker-test
  (testing "Storage worker should keep working if some Exceptions occur"
    (let [signal (promise)
          [stop store] (-> edn-storage
                           (assoc :append (fn [_] (throw (Exception.))))
                           (assoc :delete (fn [& _] (deliver signal true)))
                           worker/start)]
      ((:append store) {}) ;; this throws Exception inside worker
      (Thread/sleep 50)
      ((:delete store) {}) ;; if Exception was caught, this should gets done
      (Thread/sleep 50)
      (is signal)
      (stop))))
