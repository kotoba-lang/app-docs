(ns lg-docs.edn
  "Minimal EDN tx-op helpers for kotoba datomic — clj/bb port of lg_docs/edn.py.

  Targets the subset kotoba-server `kotoba_edn::parse` understands for
  `datomic.transact` tx-data: a vector of `[:db/add E A V]` / `[:db/retract E A V]`
  ops. In Clojure, keywords/symbols/strings/numbers ARE EDN, so `pr-str` is the
  encoder; we only add the tx-op builders + a tolerant scalar decoder for the
  server's `v_edn` reply strings."
  (:require [clojure.edn :as edn]))

(defn ->kw
  "\"doc/type\" -> :doc/type ; passes a keyword through unchanged."
  [a]
  (cond
    (keyword? a) a
    (and (string? a) (clojure.string/starts-with? a ":")) (keyword (subs a 1))
    :else (keyword a)))

(defn encode
  "EDN-encode a Clojure value to its wire string (for inlined query values)."
  [value]
  (pr-str value))

(defn tx-add
  "`[:db/add <e> <a> <v>]` tx op."
  [e a v]
  [:db/add e (->kw a) v])

(defn tx-retract
  "`[:db/retract <e> <a> <v>]` tx op."
  [e a v]
  [:db/retract e (->kw a) v])

(defn tx-retract-entity
  "`[:db.fn/retractEntity <e>]` — atomic full-entity delete."
  [e]
  [:db.fn/retractEntity e])

(defn encode-tx-data
  "Encode a sequence of tx-ops as one EDN vector string."
  [ops]
  (pr-str (vec ops)))

;; ── EDN scalar decode (server returns datom values as EDN strings) ────────────

(defn parse-edn-value
  "Decode a single EDN scalar string to a Clojure value (tolerant). A bare word
  that reads as a symbol is kept as its original string (mirrors the Python
  regex-decoder's 'unparseable stays string' behavior)."
  [s]
  (if-not (string? s)
    s
    (try
      (let [v (edn/read-string s)]
        (if (symbol? v) s v))
      (catch #?(:clj Exception :cljs :default) _ s))))
