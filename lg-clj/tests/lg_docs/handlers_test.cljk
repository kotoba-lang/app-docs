(ns lg-docs.handlers-test
  "Deterministic docs-handler + body-engine tests using the in-memory FakeDocStore.
  clj/bb port of lg/tests/test_handlers.py."
  (:require [clojure.test :refer [deftest is]]
            [lg-docs.docbody :as docbody]
            [lg-docs.handlers :as handlers]
            [lg-docs.kotoba-datomic :as kd]
            [lg-docs.store :as store]))

(defn- fresh [] (store/->fake-doc-store))
(defn- texts [doc] (mapv :text (:body doc)))

(deftest test-body-indices
  (let [body [{:text "hello"} {:text "world"}]
        idx (docbody/with-indices body)]
    (is (= 0 (:startIndex (nth idx 0))))
    (is (= 5 (:endIndex (nth idx 0))))
    (is (= 6 (:startIndex (nth idx 1))))   ; +1 newline
    (is (= 11 (:endIndex (nth idx 1))))
    (is (= "hello\nworld" (docbody/flatten-text body)))))

(deftest test-create-get
  (let [st (fresh)
        res (handlers/documents-create st {:title "Spec"})]
    (is (= "Spec" (get-in res [:document :title])))
    (is (= "rev-0" (get-in res [:document :revisionId])))
    (let [got (handlers/documents-get st {:documentId (:documentId res)})]
      (is (= true (:found got)))
      (is (= "Spec" (get-in got [:document :title]))))))

(deftest test-get-missing
  (is (= {:found false} (handlers/documents-get (fresh) {:documentId "missing01"}))))

(deftest test-append-and-heading
  (let [st (fresh)
        did (:documentId (handlers/documents-create st {:title "T"}))
        res (handlers/documents-batch-update
             st {:documentId did
                 :requests [{:op "insertHeading" :text "Intro" :headingLevel 1}
                            {:op "appendParagraph" :text "First para."}
                            {:op "appendParagraph" :text "Second para."}]})]
    (is (= true (:ok res)))
    (is (= 3 (:applied res)))
    (is (= "rev-1" (:revisionId res)))
    (let [doc (:document (handlers/documents-get st {:documentId did}))]
      (is (= ["Intro" "First para." "Second para."] (texts doc)))
      (is (= "heading" (get-in doc [:body 0 :kind])))
      (is (= 1 (get-in doc [:body 0 :headingLevel]))))))

(deftest test-replace-and-insert-text
  (let [st (fresh)
        did (:documentId (handlers/documents-create st {:title "T" :body [{:text "Hello NAME, welcome"}]}))]
    (handlers/documents-batch-update st {:documentId did :requests [{:op "replaceText" :matchText "NAME" :text "Jun"}]})
    (is (= ["Hello Jun, welcome"] (texts (:document (handlers/documents-get st {:documentId did})))))
    ;; insert at global index 5 (after "Hello")
    (handlers/documents-batch-update st {:documentId did :requests [{:op "insertText" :index 5 :text " there"}]})
    (is (= ["Hello there Jun, welcome"] (texts (:document (handlers/documents-get st {:documentId did})))))))

(deftest test-delete-range-cross-element
  (let [st (fresh)
        did (:documentId (handlers/documents-create st {:title "T" :body [{:text "AAAA"} {:text "BBBB"}]}))]
    ;; flattened "AAAA\nBBBB": delete index 2..7 ("AA|AA" .. "BB|BB") -> merge
    (handlers/documents-batch-update st {:documentId did :requests [{:op "deleteRange" :startIndex 2 :endIndex 7}]})
    (is (= ["AABB"] (texts (:document (handlers/documents-get st {:documentId did})))))))

(deftest test-revision-concurrency
  (let [st (fresh)
        did (:documentId (handlers/documents-create st {:title "T"}))
        ok (handlers/documents-batch-update st {:documentId did :requiredRevisionId "rev-0"
                                                 :requests [{:op "appendParagraph" :text "x"}]})]
    (is (= true (:ok ok)))
    (is (= "rev-1" (:revisionId ok)))
    (let [stale (handlers/documents-batch-update st {:documentId did :requiredRevisionId "rev-0"
                                                     :requests [{:op "appendParagraph" :text "y"}]})]
      (is (= {:ok false :conflict true} stale)))
    (is (= {:ok false :notFound true}
           (handlers/documents-batch-update st {:documentId "nope01" :requests []})))))

(deftest test-replace-body-sequence-used-by-ms-put
  ;; Mirrors docs-compat MS /content PUT: deleteRange(whole) + insertText(0,line0) + appendParagraph(rest).
  (let [st (fresh)
        did (:documentId (handlers/documents-create st {:title "T" :body [{:text "old line one"} {:text "old line two"}]}))
        doc (:document (handlers/documents-get st {:documentId did}))
        doc-len (reduce + (map #(+ (count (:text %)) 1) (:body doc)))
        reqs [{:op "deleteRange" :startIndex 0 :endIndex doc-len}
              {:op "insertText" :index 0 :text "alpha"}
              {:op "appendParagraph" :text "beta"}]]
    (handlers/documents-batch-update st {:documentId did :requests reqs})
    (is (= ["alpha" "beta"] (texts (:document (handlers/documents-get st {:documentId did})))))))

(deftest test-lookup-by-provider-id
  (let [st (fresh)]
    (handlers/documents-create st {:title "Imported" :googleDocumentId "gdoc_1"})
    (let [got (handlers/documents-get st {:documentId "gdoc_1"})]
      (is (= true (:found got)))
      (is (= "Imported" (get-in got [:document :title]))))))

(deftest test-kotoba-http-requires-explicit-capability
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"explicit Kotoba HTTP capability required"
                        (kd/q (kd/->client "graph") "[:find ?e]"))))

(deftest test-kotoba-host-config-and-http-capability
  (let [request (atom nil)]
    (binding [kd/*config* {:xrpc-url "https://kotoba.test/"
                           :bearer "secret"
                           :graph "docs-test"}
              kd/*post-json!* (fn [url opts]
                                (reset! request [url opts])
                                {:status 200 :body "{\"rows_edn\":[]}"})]
      (is (= [] (kd/q (kd/->client) "[:find ?e]")))
      (is (= "https://kotoba.test/xrpc/ai.etzhayyim.apps.kotoba.datomic.q"
             (first @request)))
      (is (= "Bearer secret"
             (get-in @request [1 :headers "Authorization"]))))))
