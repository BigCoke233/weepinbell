(ns weepinbell.migration-test
  (:require [weepinbell.migration :as m]
            [weepinbell.metadata.jf2-spec]
            [fast-edn.core :as edn]
            [weepinbell.storage.h2 :as h2]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io]))

(def webmention-test-edn 
  (with-open [rdr (io/reader (io/resource "webmention-test.edn"))]
    (->> rdr line-seq (map edn/read-string) doall)))

(deftest test-migration-from-mf-to-jf2
  (testing "Convert a Webmention object with microformats to one with jf2"
    (let [data (->> webmention-test-edn
                    (filter #(some? (:microformats %)))
                    first)
          converted (m/mf->jf2 data)]
      (is (contains? converted :jf2))
      (is (not (contains? converted :microformats)))
      (is (s/valid? :weepinbell.metadata.jf2-spec/jf2 
                    (:jf2 converted))))))

(deftest test-migration-from-edn-to-h2
  (testing "Migrating EDN file storage to H2 database storage"
    (let [source-edn (io/resource "webmention-test.edn")
          target-h2 "tmp/webmention-test-h2"]
      ;; assert migration succeed with no error
      (is (nil? (try
        (m/edn->h2 source-edn target-h2)
        (catch Throwable _ :fail))))
      ;; assert data count 
      (let [ds (h2/make-datasource target-h2)
            result (jdbc/execute! ds ["SELECT count(*) FROM webmention"])
            len (get (first result) (keyword "count(*)"))]
        (is (= len (count webmention-test-edn))))
      (io/delete-file (str target-h2 ".mv.db")))))
