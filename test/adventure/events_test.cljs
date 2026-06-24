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

(deftest initial-db-from-storage
  (testing "With no stored library, the initial db is seeded with the built-in demo adventures"
    (let [db1 (e/initial-db nil)]
      (is (= 2 (count (:library db1))))
      (is (= :library (:route db1)))))
  (testing "With a stored library, the initial db uses it verbatim (no re-seed)"
    (let [adv (samples/sample-adventure)
          lib {(:adventure/id adv) adv}
          db1 (e/initial-db lib)]
      (is (= lib (:library db1))))))

(deftest saving-an-adventure
  (testing "put-adventure stores an adventure in the library under its id"
    (let [adv (samples/sample-adventure)
          db1 (e/put-adventure db/default-db adv)]
      (is (= adv (get-in db1 [:library (:adventure/id adv)]))))))

(deftest opening-a-shared-link
  (let [shared (samples/sample-adventure)]
    (testing "A fresh recipient opening a link gets exactly the shared adventure and starts playing (no sample seeded)"
      (let [db1 (e/initial-db-with-share nil shared)]
        (is (= shared (get-in db1 [:library (:adventure/id shared)])))
        (is (= 1 (count (:library db1))))
        (is (= :player (:route db1)))
        (is (= [(:adventure/start shared)] (get-in db1 [:player :trail])))))
    (testing "A recipient who already has a library keeps it, with the shared adventure added"
      (let [mine (samples/sample-adventure)
            db1  (e/initial-db-with-share {(:adventure/id mine) mine} shared)]
        (is (= 2 (count (:library db1))))
        (is (= mine (get-in db1 [:library (:adventure/id mine)])))
        (is (= shared (get-in db1 [:library (:adventure/id shared)])))))
    (testing "With no incoming adventure, init behaves normally (library route, demos seeded)"
      (let [db1 (e/initial-db-with-share nil nil)]
        (is (= :library (:route db1)))
        (is (= 2 (count (:library db1))))))))

(deftest authoring-in-the-editor
  (let [adv    (samples/sample-adventure)
        adv-id (:adventure/id adv)
        db0    (seeded-db adv)]
    (testing "Starting a new adventure opens the editor with a fresh working copy; the library is untouched until save"
      (let [db1 (e/start-new-adventure db0 [::e/new])]
        (is (= :editor (:route db1)))
        (is (some? (get-in db1 [:editor :adventure])))
        (is (= 1 (count (:library db1))))))
    (testing "Editing an existing adventure loads it as the working copy with the start selected"
      (let [db1 (e/edit-adventure db0 [::e/edit adv-id])]
        (is (= adv (get-in db1 [:editor :adventure])))
        (is (= (:adventure/start adv) (get-in db1 [:editor :selected])))))
    (testing "Setting the title updates the working copy"
      (let [db1 (-> db0 (e/edit-adventure [::e/edit adv-id]) (e/editor-set-title [::e/t "Renamed"]))]
        (is (= "Renamed" (get-in db1 [:editor :adventure :adventure/title])))))
    (testing "Adding a passage adds it to the working copy and selects it"
      (let [db1 (-> db0 (e/edit-adventure [::e/edit adv-id]) (e/editor-add-passage [::e/add]))
            sel (get-in db1 [:editor :selected])]
        (is (contains? (get-in db1 [:editor :adventure :adventure/passages]) sel))))
    (testing "Saving commits the working copy to the library and clears the editor"
      (let [db1 (-> db0 (e/edit-adventure [::e/edit adv-id])
                    (e/editor-set-title [::e/t "Saved!"])
                    e/commit-working)]
        (is (= "Saved!" (get-in db1 [:library adv-id :adventure/title])))
        (is (= :library (:route db1)))
        (is (= {} (:editor db1)))))
    (testing "Cancelling discards edits and returns to the library with the original intact"
      (let [db1 (-> db0 (e/edit-adventure [::e/edit adv-id])
                    (e/editor-set-title [::e/t "nope"])
                    (e/editor-cancel nil))]
        (is (= :library (:route db1)))
        (is (= {} (:editor db1)))
        (is (= adv (get-in db1 [:library adv-id])))))
    (testing "Preview plays the working copy from its start without saving, keeping it editable"
      (let [editing (e/edit-adventure db0 [::e/edit adv-id])
            db1     (e/editor-preview editing nil)]
        (is (= :player (:route db1)))
        (is (= (:adventure/start adv) (first (get-in db1 [:player :trail]))))
        (is (= adv (get-in db1 [:player :preview-adventure])))
        (is (= adv (get-in db1 [:editor :adventure])))))
    (testing "Resuming from a preview returns to the editor and drops the preview snapshot"
      (let [db1 (-> db0 (e/edit-adventure [::e/edit adv-id])
                    (e/editor-preview nil)
                    (e/editor-resume nil))]
        (is (= :editor (:route db1)))
        (is (nil? (get-in db1 [:player :preview-adventure])))))))
