(ns lg-docs.graph-test
  "lg-docs health StateGraph (langgraph-clj) — parity with health.py."
  (:require [clojure.test :refer [deftest is]]
            [lg-docs.graph :as graph]))

(deftest test-health-probe
  (let [out (graph/run {})]
    (is (= true (:ok out)))
    (is (= "0.1.0" (:version out)))
    (is (number? (:ts out)))))

(deftest test-probe-node-direct
  (let [r (graph/probe {})]
    (is (= true (:ok r)))
    (is (= "0.1.0" (:version r)))))

(deftest test-version-is-explicitly-bound
  (binding [graph/*version* "test-version"]
    (is (= "test-version" (:version (graph/probe {}))))))
