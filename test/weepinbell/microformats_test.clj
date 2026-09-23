(ns weepinbell.microformats-test
  (:require [weepinbell.microformats.reducer :refer [reduce-mf]]
            [weepinbell.microformats.searcher :as sr]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is]]))

(def testdata
  [{:in {:items [{:type ["h-entry"]
                  :properties {:content [{:html "<div>test</div>"
                                          :text "test"}]
                               :in-reply-to ["https://test.com"]
                               :published [52341243232]
                               :author [{:type ["h-card"]
                                         :properties {:name ["test"]
                                                      :nickname ["testie"]
                                                      :photo [{:alt "testie's selfie"
                                                               :value "https://test.com/photo.png"}]
                                                      :url ["https://test.com"]}}]}}]}
    :out {:author
          {:name "test",
           :url "https://test.com",
           :photo "https://test.com/photo.png"},
          :type :in-reply-to,
          :published 52341243232,
          :content {:html "<div>test</div>", :text "test"}}}
   {:in {:items [{:type ["h-entry"]
                  :properties {:content [{:html "<div>test</div>"
                                          :text "test"}]
                               :published [52341243232]}}
                 {:type ["h-card"]
                  :properties {:name ["test"]
                               :nickname ["testie"]
                               :photo [{:alt "testie's selfie"
                                        :value "https://test.com/photo.png"}]
                               :url ["https://test.com"]}}]}
    :out {:author
          {:name "test",
           :url "https://test.com",
           :photo "https://test.com/photo.png"},
          :type :mention,
          :published 52341243232,
          :content {:html "<div>test</div>", :text "test"}}}])

(def spec-check (partial s/valid? :weepinbell.microformats.reducer/reduced-microformats))
(deftest reducer-test
  (testing "Normal microformats reduction"
    (->> testdata
         (map #(let [reduced (reduce-mf (:in %))]
                 (is (= reduced (:out %)))
                 (is (spec-check reduced))))
         doall))
  (testing "spec check invalid reduced microformats"
    (is (not (spec-check {:author nil :type :mention :published 1 :content nil})))
    (is (not (spec-check [])))))

(deftest searcher-test
  (testing "use-items gets a list of microformats items"
    (->> testdata
         (map #(is (= (sr/use-items (:in %))
                      (sr/use-items (:items (:in %)))
                      (get-in % [:in :items]))))
         doall))
  (testing "use-items returns nil if not items or cannot get items in it"
    (is (nil? (sr/use-items {:no-items nil})))
    (is (nil? (sr/use-items "no-items")))
    (is (nil? (sr/use-items [])))
    (is (nil? (sr/use-items 1))))
  (testing "use-props get properties of a microformats item"
    (->> testdata
         (map #(is (= (->> % sr/use-items sr/find-h-entry (sr/use-prop :content))
                      (get-in % [:items first :properties :content first]))))
         doall))
  (testing "use-props works with out :properties key as well"
    (is (= (sr/use-prop :key {:properties {:key ["value"]}})
           (sr/use-prop :key {:key ["value"]})
           "value")))
  (testing "find-type functions"
    (is (= ["h-entry"]
           (->> testdata first :in sr/use-items sr/find-h-entry :type)))
    (is (= ["h-card"]
           (->> testdata second :in sr/use-items sr/find-h-card :type))))
  (testing "search photos in h-card"
    (let [photo-url "https://test.com/photo.png"
          testdata-a {:type ["h-card"]
                      :properties {:photo [photo-url]}}
          testdata-b {:type ["h-card"]
                      :properties {:photo [{:alt "photo"
                                            :value photo-url}]}}
          testdata-c {:type ["h-entry"]
                      :properties {:author [{:type ["h-card"]
                                             :properties {:photo [photo-url]}}]}}
          testdata-d {:type ["h-card"]
                      :properties {:author [{:type ["h-card"]
                                             :properties {}}]
                                   :org [{:type ["h-card"]
                                          :properties {:photo [photo-url]}}]}}]
      (is (= photo-url (sr/search-photo testdata-a)))
      (is (= photo-url (sr/search-photo testdata-b)))
      (is (= photo-url (sr/search-photo testdata-c)))
      (is (= photo-url (sr/search-photo testdata-d))))))
