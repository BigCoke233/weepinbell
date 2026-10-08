;; from https://clojurians-log.clojureverse.org/clojurescript/2023-12-28/1703788493.602809
;; by p-himik, modified by Eltrac
(ns hiccup)

(defrecord Fragment [children])

(defn fragment? [x]
  (instance? Fragment x))

(defn append-child! [^js el child]
  (if (fragment? child)
    (doseq [child (:children child)
            :when (some? child)]
      (.appendChild el child))
    (.appendChild el child)))

(defn hiccup->element [hv]
  (if (vector? hv)
    (let [[tag attrs-or-child & children] hv
          attrs (when (map? attrs-or-child) attrs-or-child)
          children (cond->> children (nil? attrs) (cons attrs-or-child))]
      (if (= tag :<>)
        (->Fragment (mapv hiccup->element children))
        (let [el (js/document.createElement (name tag))]
          (doseq [[attr val] attrs]
            (if (and (keyword? attr) (= "on" (subs (name attr) 0 2)))
              (aset el (name attr) val)
              (.setAttribute el (name attr) val)))
          (doseq [child children
                  :let [child-el (hiccup->element child)]
                  :when child-el]
            (append-child! el child-el))
          el)))
    (.createTextNode js/document hv)))
