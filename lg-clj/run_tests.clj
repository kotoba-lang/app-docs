;; lg-docs clj/bb test runner (repo rule: run_tests.clj, NOT .sh).
;; Run from the app dir:  bb run_tests.clj   (or: bb test)
(require '[clojure.test :as t]
         'lg-docs.handlers-test
         'lg-docs.gitoffice-normalize-test
         'lg-docs.graph-test
         'lg-docs.server-test)

(let [{:keys [fail error]}
      (t/run-tests 'lg-docs.handlers-test
                   'lg-docs.gitoffice-normalize-test
                   'lg-docs.graph-test
                   'lg-docs.server-test)]
  (System/exit (if (pos? (+ (or fail 0) (or error 0))) 1 0)))
