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

(deftest cogbias-intro-is-valid-and-linear
  (let [adv (samples/cogbias-intro-adventure)]
    (testing "the cognitive-biases intro passes validation"
      (is (d/valid? adv)))
    (testing "it is a longer, story-driven chain ending on a terminal passage"
      (is (>= (count (:adventure/passages adv)) 8))
      ;; mostly linear: the start advances via a single choice
      (is (= 1 (count (d/choices (d/start-passage adv)))))
      ;; exactly one ending (the final passage)
      (let [endings (filter d/ending? (vals (:adventure/passages adv)))]
        (is (= 1 (count endings)))))))
