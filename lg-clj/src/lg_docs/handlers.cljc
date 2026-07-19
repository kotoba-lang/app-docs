(ns lg-docs.handlers
  "Canonical docs method handlers (ai.etzhayyim.apps.docs.*) — clj/bb port of
  lg_docs/handlers.py.

  Storage-agnostic (takes a lg-docs.store/DocStore). The docs-compat worker
  reshapes results into Google Docs v1 JSON (structural) and Microsoft Graph Word
  content (flattened plaintext). The `revisionId` (writeControl/ETag) guards
  batchUpdate concurrency. Synchronous (the Python handlers were async only
  because the store was async over httpx)."
  (:require [clojure.string :as str]
            [lg-docs.docbody :as docbody]
            [lg-docs.ids :as ids]
            [lg-docs.mapping :as mapping]
            [lg-docs.store :as store]))

(defn- now-ms []
  #?(:clj (System/currentTimeMillis) :cljs (.now js/Date)))

(defn- rev [n] (str "rev-" n))

(defn- resolve-doc [st document-id]
  (let [slug (ids/resolve-slug (or document-id ""))]
    (or
     (when slug
       (when-let [attrs (store/get-doc-attrs st slug)]
         [slug attrs]))
     (when document-id
       (some (fn [attr]
               (when-let [found (store/lookup-slug st attr document-id)]
                 (when-let [attrs (store/get-doc-attrs st found)]
                   [found attrs])))
             ["doc/googleDocumentId" "doc/msDriveItemId"]))
     [nil nil])))

(defn- rev-num [attrs]
  (let [rid (str (get attrs "doc/revisionId" "rev-0"))
        parts (str/split rid #"-" 2)]
    (try
      (if (= 2 (count parts)) #?(:clj (Integer/parseInt (second parts)) :cljs (js/parseInt (second parts))) 0)
      (catch #?(:clj Exception :cljs :default) _ 0))))

;; ── documentsCreate ───────────────────────────────────────────────────────────

(defn- document-view [attrs]
  (assoc (mapping/attrs-to-document-meta attrs)
         :body (docbody/with-indices (mapping/attrs-to-raw-body attrs))))

(defn documents-create [st inp]
  (let [slug (ids/new-slug)
        now (now-ms)
        body (vec (for [el (or (:body inp) [])]
                    (cond-> {:elementId (or (:elementId el) (ids/new-element-id))
                             :kind (or (:kind el) "paragraph")
                             :text (or (:text el) "")}
                      (some? (:headingLevel el)) (assoc :headingLevel (:headingLevel el)))))
        doc (cond-> {:title (:title inp) :revisionId (rev 0)
                     :createdAtMs now :updatedAtMs now :body body}
              (some? (:ownerDid inp)) (assoc :ownerDid (:ownerDid inp))
              (some? (:googleDocumentId inp)) (assoc :googleDocumentId (:googleDocumentId inp))
              (some? (:msDriveItemId inp)) (assoc :msDriveItemId (:msDriveItemId inp)))]
    (store/write-ops st (mapping/create-ops slug doc))
    (let [attrs (or (store/get-doc-attrs st slug) {})]
      {:documentId slug :document (document-view attrs)})))

;; ── documentsGet ──────────────────────────────────────────────────────────────

(defn documents-get [st params]
  (let [[_slug attrs] (resolve-doc st (:documentId params))]
    (if-not attrs
      {:found false}
      {:found true :document (document-view attrs)})))

;; ── documentsBatchUpdate ──────────────────────────────────────────────────────

(defn documents-batch-update [st inp]
  (let [[slug attrs] (resolve-doc st (:documentId inp))]
    (cond
      (not attrs)
      {:ok false :notFound true}

      (and (some? (:requiredRevisionId inp))
           (not= (get attrs "doc/revisionId") (:requiredRevisionId inp)))
      {:ok false :conflict true}

      :else
      (let [requests (or (:requests inp) [])
            body (reduce docbody/apply-request (mapping/attrs-to-raw-body attrs) requests)
            new-rev (rev (inc (rev-num attrs)))]
        (store/write-ops st (mapping/update-ops slug attrs
                                                {:body body :revisionId new-rev :updatedAtMs (now-ms)}))
        {:ok true :applied (count requests) :revisionId new-rev}))))
