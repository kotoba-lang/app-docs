(ns lg-docs.mapping
  "Canonical document <-> :doc/* datom mapping — clj/bb port of lg_docs/mapping.py
  (ADR-2606010500 D5).

  One document = one datomic entity `doc:doc:{slug}`. Body is an ordered list of
  structural elements stored as JSON (`:doc/bodyJson`). `:doc/revisionId` is the
  writeControl/ETag token. Attr maps are keyed by the BARE attribute string
  (\"doc/title\"), matching the store's pull shape."
  (:require [lg-docs.ids :as ids]
            [lg-docs.edn :as edn]
            #?(:clj [cheshire.core :as json])))

;; field-keyword -> bare attribute string (order-preserving)
(def scalar-fields
  [[:googleDocumentId "doc/googleDocumentId"]
   [:msDriveItemId    "doc/msDriveItemId"]
   [:title            "doc/title"]
   [:revisionId       "doc/revisionId"]
   [:ownerDid         "doc/ownerDid"]
   [:createdAtMs      "doc/createdAtMs"]
   [:updatedAtMs      "doc/updatedAtMs"]])

(def json-fields
  [[:body "doc/bodyJson"]])

(defn- dumps [v]
  #?(:clj (json/generate-string (or v []))
     :cljs (.stringify js/JSON (clj->js (or v [])))))

(defn- loads [raw default]
  (cond
    (nil? raw) default
    (not (string? raw)) raw
    :else (try
            #?(:clj (json/parse-string raw true)
               :cljs (js->clj (.parse js/JSON raw) :keywordize-keys true))
            (catch #?(:clj Exception :cljs :default) _ default))))

(defn create-ops [slug doc]
  (let [eid (ids/eid-for-slug slug)
        base [(edn/tx-add eid "doc/type" "Document")
              (edn/tx-add eid "doc/id" eid)
              (edn/tx-add eid "doc/slug" slug)]
        scalars (for [[field attr] scalar-fields
                      :when (some? (get doc field))]
                  (edn/tx-add eid attr (get doc field)))
        jsons (for [[field attr] json-fields]
                (edn/tx-add eid attr (dumps (get doc field))))]
    (vec (concat base scalars jsons))))

(defn update-ops [slug current-attrs patch]
  (let [eid (ids/eid-for-slug slug)
        scalar-ops
        (mapcat (fn [[field attr]]
                  (if-not (contains? patch field)
                    []
                    (let [new-v (get patch field)
                          old-v (get current-attrs attr)]
                      (if (= old-v new-v)
                        []
                        (cond-> []
                          (some? old-v) (conj (edn/tx-retract eid attr old-v))
                          (some? new-v) (conj (edn/tx-add eid attr new-v)))))))
                scalar-fields)
        json-ops
        (mapcat (fn [[field attr]]
                  (if-not (contains? patch field)
                    []
                    (let [new-json (dumps (get patch field))
                          old-json (get current-attrs attr)]
                      (if (= old-json new-json)
                        []
                        (cond-> []
                          (some? old-json) (conj (edn/tx-retract eid attr old-json))
                          true (conj (edn/tx-add eid attr new-json)))))))
                json-fields)]
    (vec (concat scalar-ops json-ops))))

(defn attrs-to-raw-body [attrs]
  (loads (get attrs "doc/bodyJson") []))

(defn attrs-to-document-meta
  "Document metadata WITHOUT body (handler attaches indexed body)."
  [attrs]
  (let [slug (or (get attrs "doc/slug")
                 (ids/slug-from-eid (get attrs "doc/id" "doc:doc:unknown")))]
    (reduce (fn [doc [field attr]]
              (if (some? (get attrs attr))
                (assoc doc field (get attrs attr))
                doc))
            {:documentId slug}
            scalar-fields)))
