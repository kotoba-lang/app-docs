(ns lg-docs.server-test
  "lg-docs server dispatcher (routing + x-api-key auth) over the FakeDocStore."
  (:require [clojure.test :refer [deftest is]]
            [lg-docs.server :as server]
            [lg-docs.store :as store]))

(deftest test-health-route
  (let [resp (server/handle-request (store/->fake-doc-store)
                                    {:method :get :path "/health" :headers {} :query {} :body {}})]
    (is (= 200 (:status resp)))
    (is (= true (get-in resp [:body :ok])))
    (is (= "lg-docs" (get-in resp [:body :app])))))

(deftest test-create-then-get-route
  (let [st (store/->fake-doc-store)
        created (server/handle-request st {:method :post
                                           :path "/xrpc/ai.etzhayyim.apps.docs.documentsCreate"
                                           :headers {} :query {} :body {:title "Routed"}})
        did (get-in created [:body :documentId])
        got (server/handle-request st {:method :get
                                       :path "/xrpc/ai.etzhayyim.apps.docs.documentsGet"
                                       :headers {} :query {:documentId did} :body {}})]
    (is (= 200 (:status created)))
    (is (= true (get-in got [:body :found])))
    (is (= "Routed" (get-in got [:body :document :title])))))

(deftest test-unknown-route-404
  (let [resp (server/handle-request (store/->fake-doc-store)
                                    {:method :get :path "/nope" :headers {} :query {} :body {}})]
    (is (= 404 (:status resp)))))

(deftest test-api-key-is-explicitly-bound
  (binding [server/*api-key* "secret"]
    (is (false? (server/auth-ok? {})))
    (is (true? (server/auth-ok? {"x-api-key" "secret"})))))
