(ns bellsprout.core
    (:require [bellsprout.hiccup :refer [hiccup->element]]))

;; global constants
(enable-console-print!)
(def API_BASE_URL "http://localhost:3000")

;; utilities
(defn- get-el [id]
  (.getElementById js/document id))

(defn- ms->date [ms]
  (js/Date. (js/Number ms)))
(defn- date->iso [ms]
  (.. (ms->date ms) toISOString))
(defn- date->local [ms]
  (.. (ms->date ms) toLocaleString))

;; get elements
(def input-wm-source (get-el "input-wm-source"))
(def input-wm-target (get-el "input-wm-target"))
(def button-find (get-el "find-wm"))
(def cards-area (get-el "cards"))

;; show webmentions 
(defn- create-card [js-data]
  (let [data (js->clj js-data)
        wm (get data "webmention")
        source (get wm "source")
        target (get wm "target")
        mf (get data "microformats")
        photo (or (get-in data ["extra" "proxied-photo"])
                  (get-in mf ["author" "photo"]))]
    (hiccup->element
      [:div {:class "card" :style "font-family: monospace;"}
        [:p "From: " [:a {:href source :target "_blank"} source]]
        [:p "To: " [:a {:href target :target "_blank"} target]]
        (when mf
          [:footer {:class "card-footer"}
            [:img {:src photo :class "card-photo"}]
            [:small [:a {:href (or (get-in mf ["author" "url"]) "#")
                         :target "_blank"} 
                     (get-in mf ["author" "name"])]]
            (when-let [datetime (get wm "modified")]
              (when-not (= "null" datetime)
                [:small [:time {:datetime (date->iso datetime)}
                         (date->local datetime)]]))])])))

(defn show-cards [data]
  (set! (. cards-area -innerHTML) "")
  (doseq [card-data data]
    (.appendChild cards-area 
      (create-card card-data))))

(defn get-webmention-list [by term]
  (-> (case by 
        :domain (js/fetch (str API_BASE_URL "/api/mentions/" term))
        :url    (js/fetch (str API_BASE_URL "/api/mentions?url=" term)))
      (.then (fn [resp] (.json resp)))))
(defn show-webmention-list [by term]
  (-> (get-webmention-list by term)
      (.then (fn [data] (show-cards data)))))

(defn get-webmention [source target]
  (-> (js/fetch (str API_BASE_URL "/api/mention?source=" source "&target=" target))
      (.then (fn [resp] (.json resp)))))
(defn show-webmention [source target]
  (-> (get-webmention source target)
      (.then (fn [data] (show-cards [data])))))

;; find webmention 
(defn- nil-or-empty? [value]
  (or (nil? value) (= "" value) (empty? value)))
(defn find-webmention [source target]
  (cond 
    (nil-or-empty? target) (js/alert "Cannot find webmention with no target URL")
    (nil-or-empty? source) (show-webmention-list :url target)
    :else (show-webmention source target)))

(set! (. button-find -onclick) 
      (fn [] (find-webmention (. input-wm-source -value)
                              (. input-wm-target -value))))

;; initialize states
(let [params (js/URLSearchParams. (.. js/document -location -search))
      target-preset (.get params "target")
      source-preset (.get params "source")
      show? (.get params "show")]
  ;;; use source/target preset from query params
  (set! (. input-wm-source -value) source-preset)
  (set! (. input-wm-target -value) target-preset)
  ;;; show webmentions if ?show=true 
  (when (= show? "true")
    (println "yes!")
    (find-webmention source-preset target-preset)))
