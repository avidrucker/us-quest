(ns adventure.samples-test
  "Guards the built-in demo adventure: it must be a valid, multi-level tree so it
   both works and shows off branching."
  (:require
   [cljs.test :refer [deftest is testing]]
   [adventure.domain :as d]
   [adventure.samples :as samples]))

(deftest sample-is-valid-and-branchy
  (let [adv (samples/sample-adventure)]
    (testing "the demo adventure passes validation (reachable, no dangling, has endings)"
      (is (d/valid? adv)))
    (testing "it is a real multi-level tree, not a single shallow question"
      (is (>= (count (:adventure/passages adv)) 6))
      (is (seq (d/choices (d/start-passage adv))))
      ;; at least one choice leads to a passage that itself has choices (depth >= 2)
      (let [followups (for [c (d/choices (d/start-passage adv))
                            :let [nxt (d/next-passage adv c)]
                            :when (and nxt (seq (d/choices nxt)))]
                        nxt)]
        (is (seq followups))))))
