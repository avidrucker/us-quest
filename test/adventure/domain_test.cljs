(ns adventure.domain-test
  "Behavioral specification for the pure adventure domain (BDD scenarios as
   `testing` blocks). The domain knows nothing about re-frame or the DOM."
  (:require
   [cljs.test :refer [deftest is testing]]
   [adventure.domain :as d]))

(deftest creating-an-adventure
  (testing "Given a title, a new adventure has that title, a generated id, no passages, and no start"
    (let [adv (d/new-adventure "How well do you know us?")]
      (is (= "How well do you know us?" (:adventure/title adv)))
      (is (uuid? (:adventure/id adv)))
      (is (= {} (:adventure/passages adv)))
      (is (nil? (:adventure/start adv))))))

(deftest adding-passages
  (testing "Given an adventure with no start, When the first passage is added, Then it becomes the start"
    (let [p   (d/new-passage "Where did we first meet?")
          adv (-> (d/new-adventure "Us") (d/add-passage p))]
      (is (= (:passage/id p) (:adventure/start adv)))
      (is (= p (d/start-passage adv)))
      (is (= p (d/passage adv (:passage/id p))))))
  (testing "When more passages are added, Then the start does not change and each is retrievable"
    (let [p1  (d/new-passage "first")
          p2  (d/new-passage "second")
          adv (-> (d/new-adventure "Us") (d/add-passage p1) (d/add-passage p2))]
      (is (= (:passage/id p1) (:adventure/start adv)))
      (is (= p2 (d/passage adv (:passage/id p2))))))
  (testing "Looking up a missing passage returns nil"
    (is (nil? (d/passage (d/new-adventure "Us") (random-uuid))))))

(deftest choices-and-branching
  (let [meet (d/new-passage "Where did we first meet?")
        lib  (d/new-passage "Yes! The quiet corner by the windows. 💛")
        adv  (-> (d/new-adventure "Us")
                 (d/add-passage meet)
                 (d/add-passage lib)
                 (d/add-choice (:passage/id meet)
                               {:choice/label  "The library"
                                :choice/target (:passage/id lib)}))]
    (testing "Given a choice is added, Then it is appended to its passage"
      (is (= 1 (count (d/choices (d/passage adv (:passage/id meet)))))))
    (testing "When a choice is followed, Then its target passage is returned"
      (let [choice (first (d/choices (d/passage adv (:passage/id meet))))]
        (is (= lib (d/next-passage adv choice)))))
    (testing "A passage with no choices is an ending; one with choices is not"
      (is (d/ending? lib))
      (is (not (d/ending? (d/passage adv (:passage/id meet))))))))

(deftest editing-an-adventure
  (let [p1  (d/new-passage "first")
        p2  (d/new-passage "second")
        id1 (:passage/id p1)
        id2 (:passage/id p2)
        adv (-> (d/new-adventure "Us") (d/add-passage p1) (d/add-passage p2))]
    (testing "Passage text can be changed"
      (is (= "changed" (-> adv (d/set-passage-text id1 "changed")
                           (d/passage id1) :passage/text))))
    (testing "The start can be reassigned to another passage"
      (is (= id2 (-> adv (d/set-start id2) :adventure/start))))
    (testing "A choice's target can be retargeted by index"
      (let [adv' (-> adv
                     (d/add-choice id1 {:choice/label "go" :choice/target id2})
                     (d/set-choice-target id1 0 id1))]
        (is (= id1 (-> adv' (d/passage id1) :passage/choices (get 0) :choice/target)))))
    (testing "Removing a passage deletes it, prunes choices that targeted it, and clears a start"
      (let [adv' (-> adv
                     (d/add-choice id1 {:choice/label "to p2" :choice/target id2})
                     (d/remove-passage id1))]   ; id1 is the start
        (is (nil? (d/passage adv' id1)))
        (is (nil? (:adventure/start adv'))))
      (let [adv' (-> adv
                     (d/add-choice id1 {:choice/label "to p2" :choice/target id2})
                     (d/remove-passage id2))]
        (is (nil? (d/passage adv' id2)))
        (is (empty? (d/choices (d/passage adv' id1))))))))

(deftest more-editing-for-the-author
  (let [p1  (d/new-passage "first")
        id1 (:passage/id p1)
        adv (-> (d/new-adventure "Us") (d/add-passage p1))]
    (testing "The title can be changed"
      (is (= "New Title" (-> adv (d/set-title "New Title") :adventure/title))))
    (testing "A passage image can be set"
      (is (= "💛" (-> adv (d/set-passage-image id1 "💛") (d/passage id1) :passage/image))))
    (testing "A choice's label can be changed"
      (let [adv' (-> adv
                     (d/add-choice id1 {:choice/label "x" :choice/target id1})
                     (d/set-choice-label id1 0 "Yes"))]
        (is (= "Yes" (-> adv' (d/passage id1) :passage/choices (get 0) :choice/label)))))
    (testing "A choice can be removed by index"
      (let [adv' (-> adv
                     (d/add-choice id1 {:choice/label "a" :choice/target id1})
                     (d/add-choice id1 {:choice/label "b" :choice/target id1})
                     (d/remove-choice id1 0))]
        (is (= ["b"] (mapv :choice/label (d/choices (d/passage adv' id1)))))))))

(defn- problem-types [problems]
  (set (map :problem/type problems)))

(deftest validating-an-adventure
  (testing "A reachable adventure with an ending has no problems"
    (let [meet (d/new-passage "Where did we first meet?")
          end  (d/new-passage "I love you. 💛")
          adv  (-> (d/new-adventure "Us")
                   (d/add-passage meet)
                   (d/add-passage end)
                   (d/add-choice (:passage/id meet)
                                 {:choice/label "the library" :choice/target (:passage/id end)}))]
      (is (d/valid? adv))
      (is (empty? (d/validate adv)))))
  (testing "An adventure with no start is flagged :problem/no-start"
    (is (contains? (problem-types (d/validate (d/new-adventure "Us"))) :problem/no-start)))
  (testing "A start id that names no passage is flagged :problem/dangling-start"
    (let [adv (assoc (d/new-adventure "Us") :adventure/start (random-uuid))]
      (is (contains? (problem-types (d/validate adv)) :problem/dangling-start))))
  (testing "A choice pointing nowhere is flagged :problem/dangling-choice"
    (let [meet (d/new-passage "Q?")
          adv  (-> (d/new-adventure "Us")
                   (d/add-passage meet)
                   (d/add-choice (:passage/id meet)
                                 {:choice/label "a" :choice/target (random-uuid)}))]
      (is (contains? (problem-types (d/validate adv)) :problem/dangling-choice))))
  (testing "A passage unreachable from the start is flagged :problem/unreachable"
    (let [meet   (d/new-passage "start")
          orphan (d/new-passage "nobody links here")
          adv    (-> (d/new-adventure "Us")
                     (d/add-passage meet)      ; becomes start
                     (d/add-passage orphan))]  ; never targeted
      (is (contains? (problem-types (d/validate adv)) :problem/unreachable)))))

(deftest tracing-a-path
  (testing "path-steps pairs each visited passage with the choice that led onward; the last (current) step has no chosen choice"
    (let [meet  (d/new-passage "Where did we first meet?")
          end   (d/new-passage "💛")
          c     {:choice/label "the library" :choice/target (:passage/id end)}
          adv   (-> (d/new-adventure "Us")
                    (d/add-passage meet)
                    (d/add-passage end)
                    (d/add-choice (:passage/id meet) c))
          steps (d/path-steps adv [(:passage/id meet) (:passage/id end)])]
      (is (= 2 (count steps)))
      (is (= (d/passage adv (:passage/id meet)) (:step/passage (first steps))))
      (is (= (:passage/id end) (:choice/target (:step/choice (first steps)))))
      (is (nil? (:step/choice (second steps))))
      (is (= end (:step/passage (second steps)))))))
