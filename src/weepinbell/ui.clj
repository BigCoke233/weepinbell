;; DEPRECATED !!!

(ns weepinbell.ui
  "This component is deprecated.
  Frontend will be refactored. See issue #8"
  (:require [hiccup2.core :as h]
            [weepinbell.utils :refer [format-list]]))

(defn- page-response [page]
  {:body page
   :headers {"Content-Type" "text/html"}
   :status 200})

(defn- page-base [slot]
  (str (h/html [:html
                [:head
                 [:link {:rel "stylesheet" :href "/main.css"}]
                 [:title "Weepinbell - Webmention Receiver"]]
                [:body slot]])))

(defn home [domains endpoint preset-target]
  (->
   [:main.home
    [:img.banner {:src "/weepinbell.avif"}]
    [:div
     [:p "This is a " [:a {:href "https://codeberg.org/eltrac/weepinbell" :target "_blank"} "Weepinbell"] "."]
     [:p "He receives " [:a {:href "https://indieweb.org/webmention" :target "_blank"} "Webmention"] " for " (format-list domains)]]
    [:div
     [:p "Have him send a Webmention for you."]
     [:form {:action endpoint :method "POST"}
      [:input {:name "source" :placeholder "source"}]
      [:input {:name "target" :placeholder "target"
               :value preset-target}]
      [:button {:type "submit"} "send"]]]]
   page-base
   page-response))

