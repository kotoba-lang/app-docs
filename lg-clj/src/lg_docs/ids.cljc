(ns lg-docs.ids
  "Identity helpers for documents — clj/bb port of lg_docs/ids.py.

  Canonical entity id = `doc:doc:{slug}` (the datomic entity ref).
  Path-based DID    = `did:web:docs.etzhayyim.com:document:{slug}`.
  AT URI            = `at://{did}/ai.etzhayyim.apps.docs.document/{slug}`."
  (:require [clojure.string :as str]))

(def ^:private slug-alphabet "0123456789abcdefghijklmnopqrstuvwxyz")
(def ^:private domain "docs.etzhayyim.com")
(def ^:private collection "ai.etzhayyim.apps.docs.document")

(defn- rand-str [n alphabet]
  (apply str (repeatedly n #(rand-nth alphabet))))

(defn new-slug []
  (rand-str 16 slug-alphabet))

(defn new-element-id []
  (str "el-" (rand-str 10 slug-alphabet)))

(defn eid-for-slug [slug]
  (str "doc:doc:" slug))

(defn slug-from-eid [eid]
  (last (str/split eid #":")))

(defn did-for-slug [slug]
  (str "did:web:" domain ":document:" slug))

(defn uri-for-slug [slug]
  (str "at://" (did-for-slug slug) "/" collection "/" slug))

(def ^:private slug-re #"^[0-9a-z]{6,32}$")

(defn resolve-slug
  "Normalize a documentId (slug / doc:doc: eid / did:web path) to a bare slug, or nil."
  [document-id]
  (cond
    (or (nil? document-id) (= "" document-id)) nil
    (str/starts-with? document-id "doc:doc:") (slug-from-eid document-id)
    (and (str/starts-with? document-id "did:web:")
         (str/includes? document-id ":document:"))
    (last (str/split document-id #":document:"))
    (re-matches slug-re document-id) document-id
    :else nil))
