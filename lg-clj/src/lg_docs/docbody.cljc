(ns lg-docs.docbody
  "Structural document body + batchUpdate edit engine — clj/bb port of lg_docs/docbody.py.

  The body is an ordered vector of structural elements:
    {:elementId .. :kind .. :headingLevel? .. :text ..}

  A global character index addresses the document the way Google Docs does: each
  element contributes (count text) index positions followed by one newline
  separator. `index-to-pos` maps a global index to [element-idx local-offset].

  `apply-request` returns an UPDATED body (the Python version mutated in place);
  the caller reduces over the request list. Cross-element `deleteRange` merges the
  partial start/end elements (start element's kind wins)."
  (:require [clojure.string :as str]
            [lg-docs.ids :as ids]))

(defn doc-length
  "Total global index length (text + 1 newline per element)."
  [body]
  (reduce + (map #(+ (count (get % :text "")) 1) body)))

(defn flatten-text [body]
  (str/join "\n" (map #(get % :text "") body)))

(defn with-indices
  "Return body with computed :startIndex/:endIndex per element."
  [body]
  (loop [items (seq body) pos 0 out []]
    (if-not items
      out
      (let [e (first items)
            text (get e :text "")
            item (cond-> {:elementId (get e :elementId)
                          :kind (get e :kind "paragraph")
                          :text text
                          :startIndex pos
                          :endIndex (+ pos (count text))}
                   (some? (get e :headingLevel)) (assoc :headingLevel (get e :headingLevel)))]
        (recur (next items) (+ pos (count text) 1) (conj out item))))))

(defn index-to-pos
  "Map a global index to [element-idx local-offset]. Clamps to the doc end."
  [body index]
  (loop [i 0 pos 0]
    (if (< i (count body))
      (let [tlen (count (get (nth body i) :text ""))]
        (if (<= index (+ pos tlen))
          [i (max 0 (- index pos))]
          (recur (inc i) (+ pos tlen 1))))
      (if (seq body)
        [(dec (count body)) (count (get (nth body (dec (count body))) :text ""))]
        [0 0]))))

(defn- new-paragraph
  ([text] (new-paragraph text "paragraph" nil))
  ([text kind heading-level]
   (cond-> {:elementId (ids/new-element-id) :kind kind :text text}
     (some? heading-level) (assoc :headingLevel heading-level))))

(defn- delete-range
  [body start end]
  (if (or (empty? body) (<= end start))
    body
    (let [[si so] (index-to-pos body start)
          [ei eo] (index-to-pos body end)]
      (if (= si ei)
        (let [t (get (nth body si) :text "")]
          (assoc-in body [si :text] (str (subs t 0 so) (subs t eo))))
        ;; span: keep head of start elem + tail of end elem, drop the middle, merge.
        (let [head (subs (get (nth body si) :text "") 0 so)
              tail (subs (get (nth body ei) :text "") eo)
              merged (assoc-in body [si :text] (str head tail))]
          (into (subvec merged 0 (inc si)) (subvec merged (inc ei))))))))

(defn apply-request
  "Apply one batchUpdate request, returning the updated body."
  [body req]
  (let [op (get req :op)]
    (case op
      "appendParagraph" (conj (vec body) (new-paragraph (get req :text "")))
      "insertHeading"   (conj (vec body) (new-paragraph (get req :text "") "heading" (get req :headingLevel 1)))
      "replaceText"     (let [match (get req :matchText "") repl (get req :text "")]
                          (if (seq match)
                            (mapv (fn [e]
                                    (let [t (get e :text "")]
                                      (if (str/includes? t match)
                                        (assoc e :text (str/replace t match repl))
                                        e)))
                                  body)
                            (vec body)))
      "insertText"      (let [body (if (empty? body) [(new-paragraph "")] (vec body))
                              [i off] (index-to-pos body (int (get req :index 0)))
                              t (get (nth body i) :text "")]
                          (assoc-in body [i :text] (str (subs t 0 off) (get req :text "") (subs t off))))
      "deleteRange"     (delete-range (vec body) (int (get req :startIndex 0)) (int (get req :endIndex 0)))
      (vec body))))
