(ns lg-docs.gitoffice-normalize-test
  "Tests for the docs GitOffice edge adapter (blob <-> :block/* datoms).
  clj/bb port of lg/tests/test_gitoffice_normalize.py — keeps the PARITY fixture
  so the implementations cannot drift apart silently."
  (:require [clojure.test :refer [deftest is]]
            [lg-docs.gitoffice-normalize :as gn]))

(def sample-body
  [{:elementId "el0" :kind "heading" :headingLevel 1 :text "概要"}
   {:elementId "el1" :kind "paragraph" :text "原材料費が上昇した。"}
   {:elementId "el2" :kind "listItem" :text "項目A"}
   {:elementId "el3" :kind "paragraph" :text ""}])

(deftest test-body-roundtrip-identity
  (let [ops (gn/body-to-block-ops "doc1" sample-body)
        rows (gn/ops-to-rows ops)]
    (is (= sample-body (gn/blocks-to-body rows "doc1")))))

(deftest test-normalize-is-idempotent
  (let [ops1 (gn/body-to-block-ops "doc1" sample-body)
        body2 (gn/blocks-to-body (gn/ops-to-rows ops1) "doc1")
        ops2 (gn/body-to-block-ops "doc1" body2)]
    (is (= (gn/ops-to-rows ops1) (gn/ops-to-rows ops2)))))

(deftest test-blocks-are-children-of-doc
  (let [rows (gn/ops-to-rows (gn/body-to-block-ops "doc1" sample-body))
        parents (into {} (for [[e a v] rows :when (= (gn/bare a) "block/parent")] [e v]))]
    (is (= {"el0" "doc1" "el1" "doc1" "el2" "doc1" "el3" "doc1"} parents))))

(deftest test-heading-level-only-where-present
  (let [rows (gn/ops-to-rows (gn/body-to-block-ops "doc1" sample-body))
        hl (into {} (for [[e a v] rows :when (= (gn/bare a) "block/heading-level")] [e v]))]
    (is (= {"el0" 1} hl))))

;; --- fractional indexing parity with the reference --------------------------

(deftest test-orders-strictly-increasing-and-distinct
  (let [ks (gn/initial-orders 50)]
    (is (= 50 (count ks)))
    (is (= ks (sort ks)))
    (is (= 50 (count (set ks))))))

(deftest test-order-between-inserts
  (let [[a b] (gn/initial-orders 2)
        mid (gn/order-between a b)]
    (is (and (< (compare a mid) 0) (< (compare mid b) 0)))
    (let [k1 (gn/order-between nil a)
          k2 (gn/order-between nil k1)]
      (is (and (< (compare k2 k1) 0) (< (compare k1 a) 0))))))

(deftest test-first-order-matches-cljc
  ;; first key is the base-36 midpoint of (0,1) = 'i' (18)
  (is (= "i" (first (gn/initial-orders 1)))))

(deftest test-order-between-rejects-noncanonical
  (is (thrown? Exception (gn/order-between "1" "10")))   ; trailing-zero upper bound
  (is (thrown? Exception (gn/order-between "10" nil)))   ; trailing-zero lower bound
  (is (thrown? Exception (gn/order-between "" "")))       ; empty upper bound
  (is (thrown? Exception (gn/order-between "b" "a")))     ; non-ascending
  ;; canonical shared-prefix keys still work
  (let [k (gn/order-between "1" "11")]
    (is (and (< (compare "1" k) 0) (< (compare k "11") 0)))))

(deftest test-blocks-to-body-tiebreaks-on-id
  ;; two blocks with the SAME order key (concurrent inserts) -> deterministic by id
  (let [rows [["zb" ":block/parent" "doc"] ["zb" ":block/kind" ":block/paragraph"]
              ["zb" ":block/order" "i"] ["zb" ":block/text" "B"]
              ["ya" ":block/parent" "doc"] ["ya" ":block/kind" ":block/paragraph"]
              ["ya" ":block/order" "i"] ["ya" ":block/text" "A"]]
        body (gn/blocks-to-body rows "doc")]
    (is (= ["ya" "zb"] (mapv :elementId body)))))
