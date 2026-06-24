(ns adventure.storage-test
  "Specification for library serialization (the pure half of persistence).
   The js/localStorage read/write fns are exercised in-browser, not here."
  (:require
   [cljs.test :refer [deftest is testing]]
   [adventure.samples :as samples]
   [adventure.storage :as storage]))

(deftest serialization-round-trip
  (testing "serialize then deserialize is identity for a library of adventures (uuids preserved)"
    (let [adv (samples/sample-adventure)
          lib {(:adventure/id adv) adv}]
      (is (= lib (storage/deserialize (storage/serialize lib))))))
  (testing "deserializing nothing yields nil"
    (is (nil? (storage/deserialize nil)))
    (is (nil? (storage/deserialize "")))))
