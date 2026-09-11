(ns lg-docs.graph
  "lg-docs `health` graph — clj/bb port of lg_docs/graphs/health.py onto a
  langgraph-clj StateGraph (ADR-2606280030 langgraph-python -> langgraph-clj).

  The Python health graph is a one-node liveness probe registered in langgraph.json
  (`health: ./lg_docs/graphs/health.py:GRAPH`). Same topology here:

      :probe  → {:ok true :ts <epoch-ms> :version <LG_DOCS_VERSION|0.1.0>}

  State is a Clojure map; the node fn returns a map merged into it. langgraph-clj
  loads under babashka."
  (:require [langgraph.graph :as g]))

(defn- now-ms []
  #?(:clj (System/currentTimeMillis) :cljs (.now js/Date)))

(def ^:dynamic *version* "0.1.0")

(defn- version [] *version*)

(defn probe
  "Liveness probe node — mirrors health.py `_probe`."
  [_state]
  {:ok true :ts (now-ms) :version (version)})

(defn build
  "Compile the lg-docs health StateGraph."
  []
  (-> (g/state-graph)
      (g/add-node :probe probe)
      (g/set-entry-point :probe)
      (g/set-finish-point :probe)
      (g/compile-graph)))

(defn run
  "Invoke the health graph once; returns the final state map."
  ([] (run {}))
  ([input] (g/invoke (build) input)))

;; The compiled graph (parity with health.py `GRAPH = _g.compile()`).
(def GRAPH (build))
