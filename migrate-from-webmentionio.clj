#!/usr/bin/env bb

(require '[babashka.http-client :as http])
(require '[cheshire.core :as json])

(def api-base-url "https://webmention.io/api/mentions.jf2")

(let [[token new_endpoint] *command-line-args*]
  ;; check args
  (when (or (empty? token) (empty? new_endpoint))
    (println "Usage: migrate-from-webmentionio.clj <token> <new_endpoint>")
    (System/exit 1))
  ;; page through webmention.io API
  ;; get all data and resend webmentions
  (let [api-url (str api-base-url "?token=" token)
        fetch-page (fn [page]
                     (println (str "Fetching page " page "..."))
                     (-> (http/get (str api-url "&page=" page))
                         :body
                         (json/decode true)
                         :children))
        all-data (->> (range)
                      (map fetch-page)
                      (take-while seq)
                      (apply concat))
        resend (fn [item]
                 (let [{:keys [wm-source wm-target]} item]
                   (printf "Resending: %s -> %s ... " wm-source wm-target)
                   (flush)
                   (try
                     (let [resp (http/post new_endpoint
                                           {:form-params {:source wm-source
                                                          :target wm-target}})]
                       (println (str "Done (" (:status resp) ")")))
                     (catch Exception e
                       (println (str "Failed: " (.getMessage e)))))))]
    (println "Starting migration...")
    (doseq [item all-data]
      (resend item))
    (println "Migration complete!")))
