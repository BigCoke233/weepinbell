(ns weepinbell.downloader-test
  (:require [weepinbell.http.downloader :refer [download-file]]
            [clojure.java.io :as io]
            [org.httpkit.server :refer [run-server]]
            [clojure.test :refer [deftest testing is]]))

(deftest downloader-test
  (testing "Downloading a regular text file with parent dirs not available"
    (let [handler (fn [& _] {:status 200
                             :body "file content"
                             :headers {"Content-Type" "text/plain; charset: utf-8"}})
          server (run-server handler {:port 18888 :join? false})
          file-dst (io/file "tmp/test.txt")]
      (is (download-file "http://localhost:18888" file-dst))
      (is (= "file content" (slurp file-dst)))
      (io/delete-file file-dst)
      (server)))
  (testing "Downloading a regular text file"
    (let [handler (fn [& _] {:status 200
                             :body "file content"
                             :headers {"Content-Type" "text/plain; charset: utf-8"}})
          server (run-server handler {:port 18888 :join? false})
          file-dst (io/file "tmp/test.txt")]
      (io/make-parents file-dst)
      (is (download-file "http://localhost:18888" file-dst))
      (is (= "file content" (slurp file-dst)))
      (io/delete-file file-dst)
      (server)))
  (testing "Downloading a non-existant file"
    (let [file-dst (io/file "tmp/should-not-exist.txt")]
      (is (not (download-file "https://localhost:18888" file-dst)))
      (is (not (.exists file-dst))))))

