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

(deftest seeding-and-merging-demos
  (let [built-ins (samples/built-in-adventures)
        titles    (samples/built-in-titles)
        ids       (set (map :adventure/id built-ins))]
    (testing "Brand-new visitor: an empty library seeds all built-ins; seeded = their ids"
      (let [{:keys [library seeded]} (e/seed-or-merge {} nil built-ins titles)]
        (is (= ids (set (keys library))))
        (is (= ids seeded))))
    (testing "Pre-stable-id visitor (no seeded marker): legacy auto-seeds retired, stable demos seeded, user adventures kept"
      (let [legacy (assoc (samples/sample-adventure) :adventure/id (random-uuid)) ; same title, old random id
            mine   (d/new-adventure "My own story")
            stored {(:adventure/id legacy) legacy
                    (:adventure/id mine)   mine}
            {:keys [library seeded]} (e/seed-or-merge stored nil built-ins titles)]
        (is (= ids seeded))
        (is (contains? library (:adventure/id mine)))                ; user adventure kept
        (is (every? #(contains? library %) ids))                     ; all stable demos present
        (is (= 1 (count (filter #(= "How well do you know us? 💛" (:adventure/title %))
                                (vals library)))))))                 ; legacy duplicate retired
    (testing "Returning visitor: only built-ins not yet in `seeded` are added; no duplicates"
      (let [sample (samples/sample-adventure)
            stored {(:adventure/id sample) sample}
            {:keys [library seeded]} (e/seed-or-merge stored #{(:adventure/id sample)} built-ins titles)]
        (is (= ids (set (keys library))))   ; sample + the two not-yet-seeded
        (is (= ids seeded))))
    (testing "A demo the user deleted (id in seeded, absent from library) is NOT resurrected"
      (let [mine (d/new-adventure "My own story")
            {:keys [library]} (e/seed-or-merge {(:adventure/id mine) mine} ids built-ins titles)]
        (is (= #{(:adventure/id mine)} (set (keys library))))))))

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
    (testing "A recipient who already has a (different) adventure keeps it, with the shared one added"
      (let [mine (samples/cogbias-intro-adventure)   ; distinct stable id from `shared`
            db1  (e/initial-db-with-share {(:adventure/id mine) mine} shared)]
        (is (= 2 (count (:library db1))))
        (is (= mine (get-in db1 [:library (:adventure/id mine)])))
        (is (= shared (get-in db1 [:library (:adventure/id shared)])))))
    (testing "With no incoming adventure, it returns the stored library on the library route (seeding is seed-or-merge's job)"
      (let [mine (samples/cogbias-intro-adventure)
            db1  (e/initial-db-with-share {(:adventure/id mine) mine} nil)]
        (is (= :library (:route db1)))
        (is (= {(:adventure/id mine) mine} (:library db1)))))))

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
