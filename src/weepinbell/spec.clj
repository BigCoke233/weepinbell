(ns weepinbell.spec
  (:require [clojure.spec.alpha :as s]))

(defn- contains-many? [m & ks]
  (every? #(contains? m %) ks))

(s/def ::storage
  (s/and map?
         ;; NOTE should have used clojure.spec.alpha/keys
         ;; but it requires all keys to be namespace-qualified keywords 
         ;; will have to refactor storage later
         #(contains-many? % :append :update :delete :get :get-by-domain)))
