(ns weepinbell.feed-test
  (:require [clojure.test :refer [testing is deftest]]
            [clojure.xml :as xml]
            [clojure.java.io :as io]
            [weepinbell.feed :as feed]
            [weepinbell.utils.digest :refer [digest]]))

(def domain "test.example.com")

(def storage-data
  {:normal
   [{:webmention {:source "https://test.example.net/a"
                  :target "https://test.example.com/a"
                  :modified (System/currentTimeMillis)}}
    {:webmention {:source "https://test.example.net/b"
                  :target "https://test.example.com/b"
                  :id "c6b87cf154f6d60c7ae47e2364e2d912"}}]
   :empty []})
(defn make-storage [typ]
  {:get-by-domain (fn [_] (get storage-data typ))})

(def config {:base-url "https://weep.example.com"
             :allowed-target-domains [domain]
             :feed-length 3})

(defn- reduce-to-map [xml-vector]
  (->> xml-vector
       (map (fn [data]
              {(:tag data)
               (first (:content data))}))
       (reduce conj)))

(defn- get-items [response]
  (let [feed (-> response :body .getBytes io/input-stream xml/parse)]
    (->> (:content feed)
         (filter #(= :channel (:tag %)))
         first
         :content
         (filter #(= :item (:tag %)))
         (map :content)
         (map reduce-to-map))))

(deftest rss-generation-test
  (testing "404 JSON response if domain is not allowed"
    (let [response (feed/rss "test.example.org"
                             (make-storage :normal)
                             config)]
      (is (= (:status response) 404))))
  (testing "Empty webmention feed."
    (let [response (feed/rss domain (make-storage :empty) config)]
      (is (= (count (get-items response)) 0))))
  (testing "Normal webmention feed."
    (let [response (feed/rss domain (make-storage :normal) config)
          items (get-items response)]
      (is (= (count items)
             (count (:normal storage-data))))
      (is (= (get-in storage-data [:normal 0 :webmention :source])
             (:link (first items))))
      (is (= (str (:base-url config) "/api/mention/"
                  (digest (str (get-in storage-data [:normal 0 :webmention :source])
                               (get-in storage-data [:normal 0 :webmention :target]))))
             (:guid (first items)))))))

