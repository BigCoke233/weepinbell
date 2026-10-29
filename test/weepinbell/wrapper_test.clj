(ns weepinbell.wrapper-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :refer [includes? replace]]
            [clojure.spec.alpha :as s]
            [weepinbell.utils.digest :refer [digest]]
            [weepinbell.processor.wrapper :refer [wrap]]
            [weepinbell.metadata.jf2-spec]
            [weepinbell.http.client]
            [weepinbell.utils.url :refer [url=]]
            [org.httpkit.server :refer [run-server]]
            [clojure.java.io :as io]))

(def photo-server-port 23232)

(def fake-photo (str "http://localhost:" photo-server-port "/favicon.avif"))

(def normal-source-html
  (str "<html><head><title>Name</title></head>
  <div class=h-entry>
    <div class=e-content>test</div>
    <div class=\"p-author h-card\">
      <img alt=test src=\"" fake-photo "\" />
    </div>
  </div>
  </html>"))

(deftest happy-path-wrap
  (testing "Wrapping a normal webmention"
    (with-redefs [weepinbell.http.client/default-options {:timeout 100}]
      (let [;; serve local fake photo
            photo-digest (digest fake-photo)
            local-photo (str "data/avatars/" photo-digest ".avif")
            photo-handler (fn [_] {:status 200
                                   :headers {"Content-Type" "image/png"}
                                   :body "ReaLly4IMagE"})
            photo-server (run-server photo-handler {:port photo-server-port})
            ;; test wrapping
            test-wrap (fn [] (wrap {:ok? true
                                    :wm {:s "https://source.net"
                                         :t "https://target.org"}
                                    :source-html normal-source-html}
                                   {:base-url "https://resource.com"}))
            wrapped (:new-data (test-wrap))]
        (is (= "https://source.net" (get-in wrapped [:webmention :source])))
        (is (= "https://target.org" (get-in wrapped [:webmention :target])))
        (is (= "b36497f812ccfde649996e21f8df83a6" (get-in wrapped [:webmention :id])))
        (is (s/valid? :weepinbell.metadata.jf2-spec/jf2 (:jf2 wrapped)))
        (is (.exists (io/file local-photo)))
        (is (includes? (get-in wrapped [:extra :proxied-photo]) photo-digest))
        ;; wrap again to test if using local avatars
        (is (includes? (get-in (test-wrap) [:new-data :extra :proxied-photo]) photo-digest))
        ;; clean up
        (io/delete-file local-photo)
        (photo-server))))
  (testing "Wrapping a normal webmention with relative photo-url"
    (with-redefs [weepinbell.http.client/default-options {:timeout 100}]
      (let [;; serve local fake photo
            photo-digest (digest fake-photo)
            local-photo (str "data/avatars/" photo-digest ".avif")
            photo-handler (fn [_] {:status 200
                                   :headers {"Content-Type" "image/png"}
                                   :body "ReaLly4IMagE"})
            photo-server (run-server photo-handler {:port photo-server-port})
            ;; test wrapping
            source-html (replace normal-source-html fake-photo "/favicon.avif")
            wrapped (:new-data (wrap {:ok? true
                                    :wm {:s (str "http://localhost:" photo-server-port)
                                         :t "https://target.org"}
                                    :source-html source-html}
                                   {:base-url "https://resource.com"}))]
        (is (includes? (get-in wrapped [:extra :proxied-photo]) photo-digest))
        ;; clean up
        (io/delete-file local-photo)
        (photo-server))))
  (testing "Wrapping a webmention with no photo to proxy, checking JF2"
    (let [source-html "<html><head><title>Name</title></head>
          <div class=h-entry>
            <div class=e-content>test</div>
            <div class=\"p-author h-card\">
              <a href=\"https://author.com/\" class=\"p-name u-url\">Author</a>
            </div>
          </div>
          </html>"
          wrapped (:new-data (wrap {:ok? true
                                    :wm {:s "https://source.net"
                                         :t "https://target.org"}
                                    :source-html source-html}
                                   {:base-url "https://resource.com"}))]
      (is (= "Name" (get-in wrapped [:jf2 :name])))
      (is (= "Author" (get-in wrapped [:jf2 :author :name])))
      (is (url= "https://author.com/" (get-in wrapped [:jf2 :author :url])))
      (is (= "test" (get-in wrapped [:jf2 :content :text])))))
  (testing "Wrapper uses provided id"
    (with-redefs [weepinbell.http.client/default-options {:timeout 100}]
      (let [id "totally-accurate-SHA256-hash"
            wrapped (:new-data (wrap {:ok? true
                                      :id id
                                      :wm {:s "https://source.net"
                                           :t "https://target.org"}
                                      :source-html normal-source-html}
                                     {:base-url "https://resource.com"}))]
        (is (= id (get-in wrapped [:webmention :id])))))))



