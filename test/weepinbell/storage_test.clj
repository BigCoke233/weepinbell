(ns weepinbell.storage-test
  (:require [clojure.test :refer [testing is deftest]]
            [weepinbell.storage.edn-storage :as edns]
            [weepinbell.storage.worker :as worker]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [weepinbell.storage.spec]))

;; preparing storages to test

(def test-edn-file "tmp/testfile")
(when (.exists (io/file test-edn-file))
  (io/delete-file test-edn-file))

(def edn-storage (edns/use-store test-edn-file))
(def storages [edn-storage
               (last (worker/start edn-storage))])

;; check if storages conform spec

(def spec-check (partial s/valid? :weepinbell.storage.spec/vtable))
(deftest storage-conformity-test
  (testing "edn-storage should conform storage spec."
    (with-redefs [weepinbell.storage.edn-storage/file-exists? (fn [_] true)]
      (is (spec-check (edns/use-store "source.edn")))))
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
        data [{:webmention {:source "source"
                            :target (str "https://" domain "/a")
                            :id "1"
                            :modified (System/currentTimeMillis)}}
              {:webmention {:source "source"
                            :target (str "https://" domain "/b")
                            :id "2"
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
              :microformats mf
              :extra {:proxied-photo photo}}]
    ((:append storage) data)
    (wait-til! ((:get storage) id))
    (let [retrieved ((:get storage) id)]
      (is (= photo (get-in retrieved [:extra :proxied-photo])))
      (is (= mf (:microformats retrieved))))
    ((:delete storage) id)
    (wait-til! (not ((:get storage) id)))))

(deftest general-storage-test
  (doseq [storage storages]
    (doseq [testcase [[test-append-and-get "appending webmention"]
                      [test-append-and-get-list "appending and getting by domain"]
                      [test-update "updating webmentions"]
                      [test-get-empty "making sure getting empty won't throw"]
                      [test-friends "friends of webmention (microformats, proxied-photo) can be stored correctly"]]]
      (testing (last testcase)
        ((first testcase) storage)))))

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
