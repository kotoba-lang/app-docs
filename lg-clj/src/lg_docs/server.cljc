(ns lg-docs.server
  "lg-docs server entry — clj/bb port of lg_docs/server.py.

  Routes (canonical XRPC over kotoba datomic, graph `docs-v1`):
    GET  /health /ok
    GET  /xrpc/ai.etzhayyim.apps.docs.documentsGet
    POST /xrpc/ai.etzhayyim.apps.docs.documentsCreate
    POST /xrpc/ai.etzhayyim.apps.docs.documentsBatchUpdate

  `handle-request` is a pure ring-ish dispatcher (method/path/headers/query/body
  -> {:status :body}) so the routing + x-api-key auth are deterministically
  testable. Binding it to a concrete HTTP listener (httpkit/jetty) is the one
  remaining infra leg — see the namespace docstring NOTE below.

  NOTE (coexist): the DEPLOYED appview is still the FastAPI pod (lg_docs/server.py).
  This clj dispatcher is the verified port; wiring an actual socket server under bb
  (org.httpkit.server) is deferred so the live pod is never disturbed."
  (:require [lg-docs.handlers :as handlers]
            [lg-docs.kotoba-datomic :as kd]
            [lg-docs.store :as store]))

(def ^:dynamic *api-key*
  "Host-supplied API key. Blank preserves the deployed open-gate behavior."
  "")

(defn- now-ms []
  #?(:clj (System/currentTimeMillis) :cljs (.now js/Date)))

(defn default-store
  "Production store = kotoba datomic (graph docs-v1)."
  []
  (store/->kotoba-doc-store (kd/->client)))

(defn auth-ok?
  "x-api-key gate — open when LG_DOCS_API_KEY is unset (mirrors server.py)."
  [headers]
  (or (= "" *api-key*)
      (= (get headers "x-api-key") *api-key*)))

(defn handle-request
  "Pure dispatcher. req = {:method :path :headers :query :body}. -> {:status :body}."
  [st {:keys [method path headers query body]}]
  (let [method (keyword (clojure.string/lower-case (name method)))]
    (cond
      (and (= :get method) (#{"/health" "/ok"} path))
      {:status 200 :body {:ok true :app "lg-docs" :ts (now-ms)}}

      (and (= :post method) (= path "/xrpc/ai.etzhayyim.apps.docs.documentsCreate"))
      (if-not (auth-ok? headers)
        {:status 401 :body {:detail "x-api-key mismatch"}}
        {:status 200 :body (handlers/documents-create st body)})

      (and (= :post method) (= path "/xrpc/ai.etzhayyim.apps.docs.documentsBatchUpdate"))
      (if-not (auth-ok? headers)
        {:status 401 :body {:detail "x-api-key mismatch"}}
        {:status 200 :body (handlers/documents-batch-update st body)})

      (and (= :get method) (= path "/xrpc/ai.etzhayyim.apps.docs.documentsGet"))
      (if-not (auth-ok? headers)
        {:status 401 :body {:detail "x-api-key mismatch"}}
        {:status 200 :body (handlers/documents-get st query)})

      :else
      {:status 404 :body {:detail "not found"}})))
