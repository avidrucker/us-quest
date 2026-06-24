(ns adventure.events-test
  "Specification for the player's re-frame event handlers. Handlers are pure
   db -> db functions, so we test them directly against a fixture db (no DOM)."
  (:require
   [cljs.test :refer [deftest is testing]]
   [adventure.db :as db]
   [adventure.domain :as d]
   [adventure.samples :as samples]
   [adventure.events :as e]))

(defn- seeded-db [adv]
  (assoc-in db/default-db [:library (:adventure/id adv)] adv))

(deftest playing-through
  (let [adv    (samples/sample-adventure)
        adv-id (:adventure/id adv)
        start  (:adventure/start adv)
        db0    (seeded-db adv)]
    (testing "Starting a playthrough routes to the player and begins the trail at the start"
      (let [db1 (e/start-playthrough db0 [::e/start adv-id])]
        (is (= :player (:route db1)))
        (is (= adv-id (get-in db1 [:player :adventure-id])))
        (is (= [start] (get-in db1 [:player :trail])))))
    (testing "Choosing appends the choice's target to the trail"
      (let [db1    (e/start-playthrough db0 [::e/start adv-id])
            choice (first (d/choices (d/start-passage adv)))
            db2    (e/choose db1 [::e/choose choice])]
        (is (= [start (:choice/target choice)] (get-in db2 [:player :trail])))))
    (testing "Going back pops the trail but never past the start"
      (let [choice (first (d/choices (d/start-passage adv)))
            db2    (-> db0 (e/start-playthrough [::e/start adv-id]) (e/choose [::e/choose choice]))
            db3    (e/go-back db2 [::e/back])
            db4    (e/go-back db3 [::e/back])]
        (is (= [start] (get-in db3 [:player :trail])))
        (is (= [start] (get-in db4 [:player :trail])))))
    (testing "Restart returns the trail to just the start"
      (let [choice (first (d/choices (d/start-passage adv)))
            db2    (-> db0 (e/start-playthrough [::e/start adv-id])
                       (e/choose [::e/choose choice]) (e/restart [::e/restart]))]
        (is (= [start] (get-in db2 [:player :trail])))))))

(deftest initializing
  (testing "initialize-db seeds the library with the sample adventure and lands on the library route"
    (let [db1 (e/initialize-db nil nil)]
      (is (= 1 (count (:library db1))))
      (is (= :library (:route db1))))))
