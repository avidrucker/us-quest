(ns adventure.smoke-test
  "Trivial test proving the test runner is wired up. Real domain tests land in
   Milestone 2 (adventure.domain-test)."
  (:require
   [cljs.test :refer [deftest is testing]]))

(deftest scaffolding-works
  (testing "the test runner compiles and runs"
    (is (= 4 (+ 2 2)))))
