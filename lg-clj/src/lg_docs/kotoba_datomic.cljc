(ns lg-docs.kotoba-datomic
  "kotoba datomic XRPC client — clj/bb port of lg_docs/kotoba_datomic.py
  (httpx -> babashka.http-client, per the actor-tree httpx->bb ports #2612).

  Wraps `ai.etzhayyim.apps.kotoba.datomic.{transact,q,pull}` — the canonical
  write/read surface for documents (graph `docs-v1`). Endpoint resolution honors
  `KOTOBA_XRPC_URL`/`KOTOBA_URL`; auth = Bearer JWT (`KOTOBA_BEARER`).

  Read-only `q`/`pull` carry no server key by default (no-server-key, read-only,
  ADR-2606072802); a write `transact` needs the operator/member bearer."
  (:require [clojure.string :as str]
            [lg-docs.edn :as edn]
            #?(:clj [cheshire.core :as json]))
  #?(:clj (:import [java.security MessageDigest])))

(def ^:dynamic *config*
  {:xrpc-url "http://kotoba.kotoba.svc.cluster.local:8080"
   :bearer ""
   :graph "docs-v1"})

(def ^:dynamic *post-json!*
  (fn [& _]
    (throw (ex-info "explicit Kotoba HTTP capability required"
                    {:capability :kotoba-http}))))

;; ── content addressing (kotoba CIDv1, raw/sha2-256, multibase base32) ─────────

(def ^:private b32-alphabet "abcdefghijklmnopqrstuvwxyz234567")

(defn- base32-lower
  "RFC4648 base32 (lowercase, no padding) of a byte array."
  [^bytes data]
  #?(:clj
     (let [bs (map #(bit-and % 0xff) data)]
       (loop [bits 0 val 0 bytes-left bs out (StringBuilder.)]
         (if (and (empty? bytes-left) (< bits 5))
           (str out)
           (if (>= bits 5)
             (recur (- bits 5) val bytes-left
                    (.append out (nth b32-alphabet (bit-and (bit-shift-right val (- bits 5)) 0x1f))))
             (let [val (bit-or (bit-shift-left val 8) (first bytes-left))]
               (recur (+ bits 8) val (rest bytes-left) out))))))
     :cljs (throw (js/Error. "base32 not implemented for cljs"))))

(defn kotoba-cid [^bytes payload]
  #?(:clj
     (let [digest (.digest (MessageDigest/getInstance "SHA-256") payload)
           prefix (byte-array [0x01 0x71 0x12 0x20])
           cid (byte-array (concat (seq prefix) (seq digest)))]
       (str "b" (base32-lower cid)))
     :cljs (throw (js/Error. "kotoba-cid not implemented for cljs"))))

(defn graph-cid-for-label
  "Stable kotoba graph CID from a human-readable label (multibase passthrough)."
  [label]
  (if (and (str/starts-with? label "b") (re-matches #"b[a-z2-7]{58,80}" label))
    label
    #?(:clj (kotoba-cid (.getBytes ^String label "UTF-8")) :cljs label)))

(defn- headers []
  (if (seq (:bearer *config*))
    {"Authorization" (str "Bearer " (:bearer *config*))}
    {}))

#?(:clj
   (defn- post-json [path body]
     (*post-json!*
      (str (str/replace (:xrpc-url *config*) #"/+$" "") path)
      {:headers (merge {"Content-Type" "application/json"} (headers))
       :body (json/generate-string body)
       :throw false})))

;; ── pull datom folding ────────────────────────────────────────────────────────

(defn- bare-attr [a]
  (let [s (str a)] (if (str/starts-with? s ":") (subs s 1) s)))

(defn datoms->attr-map
  "Fold pull datoms ({:a :v_edn :added}) into {bare-attr value}, dropping retractions."
  [datoms]
  (when (seq datoms)
    (let [out (reduce (fn [m d]
                        (if (or (not (map? d)) (false? (get d :added)))
                          m
                          (let [a (get d :a)]
                            (if a
                              (assoc m (bare-attr a) (edn/parse-edn-value (get d :v_edn "")))
                              m))))
                      {} datoms)]
      (when (seq out) out))))

;; ── client record ─────────────────────────────────────────────────────────────

(defrecord KotobaDatomic [graph])

(defn ->client
  ([] (->KotobaDatomic (graph-cid-for-label (:graph *config*))))
  ([graph] (->KotobaDatomic graph)))

(defn transact
  ([dm ops] (transact dm ops nil))
  ([dm ops expected-parent]
   #?(:clj
      (let [body (cond-> {:graph (:graph dm) :tx_edn (edn/encode-tx-data ops)}
                   expected-parent (assoc :expected_parent expected-parent))
            resp (post-json "/xrpc/ai.etzhayyim.apps.kotoba.datomic.transact" body)]
        (when (>= (:status resp) 400)
          (throw (ex-info "kotoba transact failed" {:status (:status resp) :body (:body resp)})))
        (json/parse-string (:body resp) true))
      :cljs (throw (js/Error. "transact not implemented for cljs")))))

(defn q
  ([dm query-edn] (q dm query-edn nil))
  ([dm query-edn inputs-edn]
   #?(:clj
      (let [body (cond-> {:graph (:graph dm) :query_edn query-edn}
                   (seq inputs-edn) (assoc :inputs_edn inputs-edn))
            resp (post-json "/xrpc/ai.etzhayyim.apps.kotoba.datomic.q" body)]
        (when (>= (:status resp) 400)
          (throw (ex-info "kotoba q failed" {:status (:status resp) :body (:body resp)})))
        (let [rows (or (get (json/parse-string (:body resp) true) :rows_edn) [])]
          (mapv (fn [row] (mapv edn/parse-edn-value row)) rows)))
      :cljs (throw (js/Error. "q not implemented for cljs")))))

(defn pull
  [dm entity]
  #?(:clj
     (let [body {:graph (:graph dm) :entity entity}
           resp (post-json "/xrpc/ai.etzhayyim.apps.kotoba.datomic.pull" body)]
       (cond
         (= 404 (:status resp)) nil
         (>= (:status resp) 400) (throw (ex-info "kotoba pull failed" {:status (:status resp)}))
         :else (datoms->attr-map (or (get (json/parse-string (:body resp) true) :datoms) []))))
     :cljs (throw (js/Error. "pull not implemented for cljs"))))
