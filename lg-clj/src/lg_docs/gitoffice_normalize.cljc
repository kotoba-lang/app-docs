(ns lg-docs.gitoffice-normalize
  "GitOffice edge adapter — :doc/bodyJson blob <-> element-granular :block/* datoms.
  clj/bb port of lg_docs/gitoffice_normalize.py.

  Order is a fractional-index string (insert-friendly), byte-for-byte matching the
  reference gitoffice.cljc + the kotoba CLJS port. The elementId IS the stable
  block id, so re-normalizing is idempotent (parity asserted in tests)."
  (:require [clojure.string :as str]
            [lg-docs.edn :as edn]))

;; --- fractional indexing (base-36 fractions, lexicographic order) -----------

(def ^:private digits "0123456789abcdefghijklmnopqrstuvwxyz")
(def ^:private base (count digits))
(def ^:private dval (into {} (map-indexed (fn [i c] [c i]) digits)))

(defn order-between
  "Fractional index strictly between a (lower, nil/\"\" = 0) and b (upper, nil = 1).
  Keys must be CANONICAL (no trailing '0'); a non-canonical key is rejected loudly."
  [a b]
  (let [a (or a "")]
    (when (and (seq a) (= \0 (last a)))
      (throw (ex-info (str "invalid order key (trailing zero): " (pr-str a)) {})))
    (when (some? b)
      (when (or (= "" b) (= \0 (last b)))
        (throw (ex-info (str "invalid order key (trailing zero / empty): " (pr-str b)) {})))
      (when (>= (compare a b) 0)
        (throw (ex-info (str "order keys not ascending: " (pr-str a) " >= " (pr-str b)) {}))))
    (loop [i 0 acc []]
      (let [da (if (< i (count a)) (dval (nth a i)) 0)
            db (if (and b (< i (count b))) (dval (nth b i)) base)]
        (if (< (+ da 1) db)
          (apply str (map #(nth digits %) (conj acc (quot (+ da db) 2))))
          (recur (inc i) (conj acc da)))))))

(defn initial-orders
  "n strictly-increasing order keys (one per body position)."
  [n]
  (loop [k n prev nil out []]
    (if (zero? k)
      out
      (let [o (order-between prev nil)]
        (recur (dec k) o (conj out o))))))

;; --- kind mapping -----------------------------------------------------------

(def ^:private kind->kw {"paragraph" "block/paragraph" "heading" "block/heading"
                         "listItem" "block/list-item"})
(def ^:private kw->kind (into {} (map (fn [[k v]] [v k]) kind->kw)))

(defn bare [a]
  (let [s (str a)] (if (str/starts-with? s ":") (subs s 1) s)))

(defn- bare-kw-val [v]
  (let [s (str v)] (if (str/starts-with? s ":") (subs s 1) s)))

;; --- blob -> datom ops ------------------------------------------------------

(defn body-to-block-ops
  "`:doc/bodyJson` element list -> `[:db/add e a v]` ops for store/write-ops."
  [doc-id body]
  (let [orders (initial-orders (count body))]
    (vec (mapcat (fn [el order]
                   (let [bid (:elementId el)
                         base-ops [(edn/tx-add bid "block/parent" doc-id)
                                   (edn/tx-add bid "block/kind"
                                               (keyword (kind->kw (or (:kind el) "paragraph") "block/paragraph")))
                                   (edn/tx-add bid "block/order" order)
                                   (edn/tx-add bid "block/text" (or (:text el) ""))]]
                     (if (some? (:headingLevel el))
                       (conj base-ops (edn/tx-add bid "block/heading-level" (:headingLevel el)))
                       base-ops)))
                 body orders))))

;; --- datom rows -> blob -----------------------------------------------------

(defn blocks-to-body
  "Decoded (e a v) rows -> `:doc/bodyJson` element list (sorted by block order, id tie-break)."
  [rows doc-id]
  (let [by-block (reduce (fn [m [e a v]]
                           (let [attr (bare a)]
                             (if (and (str/starts-with? attr "block/")
                                      (not (and (= attr "block/parent") (not= v doc-id))))
                               (assoc-in m [e attr] v)
                               m)))
                         {} rows)
        blocks (into {} (filter (fn [[_ attrs]] (= (get attrs "block/parent") doc-id)) by-block))
        ordered (sort-by (fn [bid] [(get-in blocks [bid "block/order"] "") bid]) (keys blocks))]
    (vec (for [bid ordered
               :let [attrs (get blocks bid)]]
           (cond-> {:elementId bid
                    :kind (kw->kind (bare-kw-val (get attrs "block/kind" "block/paragraph")) "paragraph")
                    :text (get attrs "block/text" "")}
             (contains? attrs "block/heading-level")
             (assoc :headingLevel (get attrs "block/heading-level")))))))

(defn ops-to-rows
  "`[:db/add e a v]` ops -> (e a v) rows (drops the op verb)."
  [ops]
  (vec (for [op ops :when (= 4 (count op))] [(nth op 1) (nth op 2) (nth op 3)])))
