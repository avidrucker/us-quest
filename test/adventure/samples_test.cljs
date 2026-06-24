(ns adventure.samples-test
  "Guards the built-in demo adventure: it must be a valid, multi-level tree so it
   both works and shows off branching."
  (:require
   [clojure.string :as str]
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

(deftest japanese-intro-is-valid-and-teaches-by-looping
  (let [adv      (samples/japanese-intro-adventure)
        start    (d/start-passage adv)
        start-id (:adventure/start adv)
        passages (vals (:adventure/passages adv))
        blob     (str/join " " (concat (map :passage/text passages)
                                       (mapcat #(map :choice/label (:passage/choices %)) passages)))]
    (testing "the Japanese intro passes validation (reachable, no dangling, has an ending)"
      (is (d/valid? adv)))
    (testing "it teaches the three target phrases"
      (is (str/includes? blob "はじめまして"))
      (is (str/includes? blob "です"))
      (is (str/includes? blob "よろしく")))
    (testing "the first question offers a correct answer plus wrong ones"
      (is (>= (count (d/choices start)) 3)))
    (testing "exactly one ending (the success payoff)"
      (is (= 1 (count (filter d/ending? passages)))))
    (testing "wrong answers loop back: at least one choice leads to a reaction whose only option returns to that same question"
      (let [loops (for [c   (d/choices start)
                        :let [r (d/next-passage adv c)]
                        :when (and r
                                   (= 1 (count (d/choices r)))
                                   (= start-id (:choice/target (first (d/choices r)))))]
                    r)]
        (is (seq loops))))))

(deftest built-ins-have-stable-distinct-ids
  (testing "each built-in returns the same :adventure/id across calls (stable identity)"
    (is (= (:adventure/id (samples/sample-adventure))   (:adventure/id (samples/sample-adventure))))
    (is (= (:adventure/id (samples/cogbias-intro-adventure))  (:adventure/id (samples/cogbias-intro-adventure))))
    (is (= (:adventure/id (samples/japanese-intro-adventure)) (:adventure/id (samples/japanese-intro-adventure))))
    (is (= (:adventure/id (samples/dungeon-crawl-adventure))  (:adventure/id (samples/dungeon-crawl-adventure)))))
  (testing "the built-ins have distinct ids"
    (let [ids (map :adventure/id (samples/built-in-adventures))]
      (is (= (count ids) (count (set ids)))))))

(deftest dungeon-crawl-is-valid-with-many-endings
  (let [adv      (samples/dungeon-crawl-adventure)
        passages (vals (:adventure/passages adv))
        endings  (filter d/ending? passages)
        ending-blob (str/join " " (map :passage/text endings))]
    (testing "the dungeon crawl passes validation (reachable, no dangling targets)"
      (is (d/valid? adv)))
    (testing "it is a substantial tree with several endings"
      (is (>= (count passages) 14))
      (is (>= (count endings) 5)))
    (testing "the start offers multiple branches (incl. the back-to-tavern out)"
      (is (>= (count (d/choices (d/start-passage adv))) 3)))
    (testing "it includes a secret 'friend' ending and at least one death ending"
      (is (re-find #"(?i)friend" ending-blob))
      (is (str/includes? ending-blob "💀")))
    (testing "paths converge: the cavern is reachable from more than one passage"
      (let [cavern-targets (for [p passages
                                 c (:passage/choices p)
                                 :let [t (d/passage adv (:choice/target c))]
                                 :when (str/includes? (:passage/text t) "sleeps atop a mountain of gold")]
                             (:passage/id p))]
        (is (>= (count (distinct cavern-targets)) 2))))))
