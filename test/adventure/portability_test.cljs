(ns adventure.portability-test
  "Specification for file export/import: an adventure must survive a round-trip
   through the .edn file form unchanged, and bad input must be rejected (never
   throw)."
  (:require
   [cljs.test :refer [deftest is testing]]
   [adventure.domain :as d]
   [adventure.portability :as p]
   [adventure.samples :as samples]))

(deftest export-import-round-trip
  (testing "export then parse-imported is identity (ids, emoji, structure preserved)"
    (let [adv (samples/sample-adventure)]
      (is (= {:ok adv} (p/parse-imported (p/export-edn adv))))))
  (testing "a shaped-but-not-yet-play-valid adventure is still importable (prototype)"
    (let [proto (d/new-adventure "Work in progress")]  ; no passages, nil start
      (is (not (d/valid? proto)))
      (is (= {:ok proto} (p/parse-imported (p/export-edn proto)))))))

(deftest filename-slug
  (testing "slugifies the title and ends in .edn"
    (is (= "hajimemashite-a-first-meeting-in-japanese.edn"
           (p/filename {:adventure/title "Hajimemashite! A first meeting in Japanese 🇯🇵"}))))
  (testing "a title with no slug-able characters falls back to adventure.edn"
    (is (= "adventure.edn" (p/filename {:adventure/title "🇯🇵💛"})))))

(deftest rejects-bad-input
  (testing "unreadable / empty input yields an :error, never an exception"
    (is (:error (p/parse-imported "{:not closed")))
    (is (:error (p/parse-imported ""))))
  (testing "well-formed EDN that isn't an adventure is rejected"
    (is (:error (p/parse-imported "{:hello \"world\"}")))
    (is (:error (p/parse-imported
                 (pr-str {:adventure/id "not-a-uuid" :adventure/title "x"
                          :adventure/passages {}})))))) ; id isn't a uuid
