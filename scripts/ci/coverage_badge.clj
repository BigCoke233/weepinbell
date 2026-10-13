(ns ci.coverage-badge
  (:require [hickory.core :as h]
            [clojure.string :as string]))

(def readme-path "README.md")
(def regex #"(?s)(?<=<!--COVERAGE_BADGE:START-->).*?(?=<!--COVERAGE_BADGE:END-->)")

(def cloverage-output "target/coverage/index.html")
(def coverage (let [rows (-> cloverage-output
                             slurp
                             h/parse
                             h/as-hickory
                             :content first ;; get <html>
                             :content (nth 3) ;; get <body> 
                             :content second ;; get <table>
                             :content last ;; get <tbody>
                             :content)
                    total-row (->> rows
                                   (map :content)
                                   (filter sequential?)
                                   (filter #(= "Totals:" (get-in % [0 :content 0])))
                                   first
                                   (filter map?))]
                (-> total-row
                    (nth 2)
                    :content
                    first
                    (string/replace " " ""))))
;; "25" will make "%25" which decoded to "%", removing it will result in a Bad Request 
(def badge-url (str "https://img.shields.io/badge/coverage-" coverage "25-green"))
(def badge-html
  (str "<img src=\"" badge-url "\" alt=\"test coverage " coverage "\">"))

(defn -main [& _]
  (let [readme-old (slurp readme-path)
        readme-new (string/replace readme-old regex badge-html)]
    (spit readme-path readme-new)))
