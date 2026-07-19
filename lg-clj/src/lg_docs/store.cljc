(ns lg-docs.store
  "Docs persistence stores — clj/bb port of lg_docs/store.py.

  `DocStore` is the narrow protocol the handlers use. Two implementations:

  - `->KotobaDocStore` — production, backed by kotoba datomic (graph `docs-v1`)
    via lg-docs.kotoba-datomic/KotobaDatomic.
  - `->FakeDocStore` — in-memory atom, used by the unit/smoke tests so the
    canonical handler logic is verified deterministically without a live pod.

  Both speak the same EDN tx-op vocabulary (`[:db/add ..]` / `[:db/retract ..]` /
  `[:db.fn/retractEntity ..]`). Attr maps are keyed by the BARE attribute string."
  (:require [lg-docs.ids :as ids]
            [lg-docs.edn :as edn]
            [lg-docs.kotoba-datomic :as kd]))

(defprotocol DocStore
  (get-doc-attrs [this slug])
  (all-doc-attrs [this])
  (lookup-slug [this attr value])
  (write-ops [this ops]))

(defn- bare [a]
  (let [s (if (keyword? a) (subs (str a) 1) (str a))]
    (if (clojure.string/starts-with? s ":") (subs s 1) s)))

;; ── kotoba datomic implementation ─────────────────────────────────────────────

(defrecord KotobaDocStore [dm]
  DocStore
  (get-doc-attrs [_ slug]
    (kd/pull dm (ids/eid-for-slug slug)))
  (all-doc-attrs [this]
    (let [rows (kd/q dm "[:find ?slug :where [?e :doc/type \"Document\"] [?e :doc/slug ?slug]]")]
      (vec (for [row rows
                 :when (seq row)
                 :let [attrs (get-doc-attrs this (str (first row)))]
                 :when attrs]
             attrs))))
  (lookup-slug [_ attr value]
    ;; Inline the EDN-encoded value into the query (proven get_entity pattern).
    (let [b (bare attr)
          query (str "[:find ?slug :where [?e :" b " " (edn/encode value) "] [?e :doc/slug ?slug]]")
          rows (kd/q dm query)]
      (when (and (seq rows) (seq (first rows)))
        (str (ffirst rows)))))
  (write-ops [_ ops]
    (when (seq ops)
      (kd/transact dm ops))))

(defn ->kotoba-doc-store [dm]
  (->KotobaDocStore dm))

;; ── in-memory fake (tests) ────────────────────────────────────────────────────

(defrecord FakeDocStore [db]
  DocStore
  (get-doc-attrs [_ slug]
    (get @db slug))
  (all-doc-attrs [_]
    (vec (vals @db)))
  (lookup-slug [_ attr value]
    (let [b (bare attr)]
      (some (fn [[slug attrs]] (when (= (get attrs b) value) slug)) @db)))
  (write-ops [_ ops]
    (doseq [op ops]
      (let [kind (first op)]
        (if (= kind :db.fn/retractEntity)
          (let [slug (ids/slug-from-eid (str (nth op 1)))]
            (swap! db dissoc slug))
          (let [eid (nth op 1) attr (nth op 2) value (nth op 3)
                b (bare attr)
                slug (ids/slug-from-eid (str eid))]
            (cond
              (= kind :db/add)
              (swap! db update slug assoc b value)
              (= kind :db/retract)
              (swap! db update slug
                     (fn [row] (if (= (get row b) value) (dissoc row b) row))))))))))

(defn ->fake-doc-store []
  (->FakeDocStore (atom {})))
