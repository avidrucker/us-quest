(ns adventure.share-test
  "Specification for share-link encoding: an adventure must survive a round-trip
   through the URL-safe string form unchanged (ids and emoji included)."
  (:require
   [cljs.test :refer [deftest is testing]]
   [adventure.samples :as samples]
   [adventure.share :as share]))

(deftest encode-decode-round-trip
  (testing "encode then decode is identity (uuids and emoji preserved)"
    (let [adv (samples/sample-adventure)]
      (is (= adv (share/decode (share/encode adv))))))
  (testing "the encoded form is URL-safe (no +, /, or whitespace)"
    (let [encoded (share/encode (samples/sample-adventure))]
      (is (not (re-find #"[+/\s]" encoded)))))
  (testing "decoding nil or garbage yields nil rather than throwing"
    (is (nil? (share/decode nil)))
    (is (nil? (share/decode "")))
    (is (nil? (share/decode "@@@not-valid@@@")))))
