(ns weepinbell.wrapper-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :refer [includes?]]
            [weepinbell.utils.digest :refer [digest]]
            [weepinbell.processor.wrapper :refer [wrap]]))

(def normal-source-html
  "<div class=h-entry>
    <div class=e-content>test</div>
    <div class=\"p-author h-card\">
      <img alt=test src=\"https://source.net/favicon.png\" />
    </div>
  </div>")

(deftest happy-path-wrap
  (testing "Wrapping a normal webmention"
    (let [wrapped (:new-data (wrap {:ok? true
                                    :wm {:s "https://source.net"
                                         :t "https://target.org"}
                                    :source-html normal-source-html}
                                   {:base-url "https://resource.com"}))]
      (is (= "https://source.net" (get-in wrapped [:webmention :source])))
      (is (= "https://target.org" (get-in wrapped [:webmention :target])))
      (is (= "b36497f812ccfde649996e21f8df83a6" (get-in wrapped [:webmention :id])))
      (is (includes? (get-in wrapped [:extra :proxied-photo]) (digest "https://source.net/favicon.png"))))))
