(ns weepinbell.metadata.jf2
  "JF2 parser from various data formats (eg. microformats) to JF2 format."
  (:require [hickory.core :as hickory]
            [mf2clj.core :as mf]
            [weepinbell.metadata.microformats :refer [parse-microformats]]
            [weepinbell.metadata.html-head :refer [parse-meta]]
            [weepinbell.metadata.json-ld :refer [parse-json-ld]]))

(defn supplement-merge
  "Deep merge maps A and B where B never overrides A.
   Supplements A with values A does not have but B has."
  [a b]
  (let [supplement (fn [acc k v]
                     (assoc acc k (supplement-merge (get acc k) v)))]
    (cond
      (nil? a)           b
      (not (and (map? a) (map? b))) a
      :else              (reduce-kv supplement a b))))

(defn parse
  "Takes HTML string, return a JF2 data object compiled from
   mircorformats and more sources."
  [^String html-string]
  {:pre [(string? html-string)]}
  (let [mf (mf/parse html-string)
        mf->jf2 (parse-microformats mf)
        hick (-> html-string hickory/parse hickory/as-hickory)
        more [mf->jf2
              (parse-meta hick)
              (parse-json-ld hick)]]
    (reduce supplement-merge more)))

