(ns adventure.domain
  "The pure heart of Adventures in Us: the data model for branching adventures
   and the functions that build, query, and validate them.

   An **adventure** is a flat map of **passages** keyed by id, plus a designated
   **start** passage. A **choice** on a passage is a labelled branch to another
   passage. A passage with no choices is an **ending**.

   This namespace has no dependency on re-frame or the DOM — it is data and pure
   functions only, fully unit-testable in isolation."
  (:require
   [com.fulcrologic.guardrails.malli.core :refer [>defn >defn- >def => ?]]))

;; ---------------------------------------------------------------------------
;; Schemas (ubiquitous language, registered for use in gspecs)
;; ---------------------------------------------------------------------------

(>def :adventure/id :uuid)
(>def :adventure/title :string)
(>def :passage/id :uuid)
(>def :passage/text :string)
(>def :choice/label :string)
(>def :choice/target :uuid)

(>def :adventure/choice
  [:map
   [:choice/label :choice/label]
   ;; target is optional while authoring (a choice may not point anywhere yet)
   [:choice/target {:optional true} (? :choice/target)]])

(>def :adventure/passage
  [:map
   [:passage/id :passage/id]
   [:passage/text :passage/text]
   [:passage/image {:optional true} :string]
   [:passage/choices [:vector :adventure/choice]]])

(>def :adventure/adventure
  [:map
   [:adventure/id :adventure/id]
   [:adventure/title :adventure/title]
   [:adventure/start (? :passage/id)]
   [:adventure/passages [:map-of :passage/id :adventure/passage]]])

;; ---------------------------------------------------------------------------
;; Construction
;; ---------------------------------------------------------------------------

(>defn new-adventure
  "Returns a fresh, empty adventure with the given `title`, a generated id, no
   passages, and no start."
  [title]
  [:adventure/title => :adventure/adventure]
  {:adventure/id       (random-uuid)
   :adventure/title    title
   :adventure/start    nil
   :adventure/passages {}})

(>defn new-passage
  "Returns a fresh passage containing `text` and no choices."
  [text]
  [:passage/text => :adventure/passage]
  {:passage/id      (random-uuid)
   :passage/text    text
   :passage/choices []})

(>defn add-passage
  "Adds `passage` to `adventure`. If the adventure has no start yet, the new
   passage also becomes the start."
  [adventure passage]
  [:adventure/adventure :adventure/passage => :adventure/adventure]
  (let [id (:passage/id passage)]
    (cond-> (assoc-in adventure [:adventure/passages id] passage)
      (nil? (:adventure/start adventure)) (assoc :adventure/start id))))

;; ---------------------------------------------------------------------------
;; Queries / traversal
;; ---------------------------------------------------------------------------

(>defn passage
  "Returns the passage in `adventure` identified by `passage-id`, or nil."
  [adventure passage-id]
  [:adventure/adventure :passage/id => (? :adventure/passage)]
  (get-in adventure [:adventure/passages passage-id]))

(>defn start-passage
  "Returns the start passage of `adventure`, or nil if none is set."
  [adventure]
  [:adventure/adventure => (? :adventure/passage)]
  (some->> (:adventure/start adventure) (passage adventure)))

(>defn choices
  "Returns the vector of choices on `the-passage` (possibly empty)."
  [the-passage]
  [:adventure/passage => [:vector :adventure/choice]]
  (or (:passage/choices the-passage) []))

(>defn ending?
  "True when `the-passage` has no choices — a terminal passage (the payoff)."
  [the-passage]
  [:adventure/passage => :boolean]
  (empty? (:passage/choices the-passage)))

(>defn next-passage
  "Returns the passage that `choice` leads to within `adventure`, or nil when the
   choice has no target or its target is dangling."
  [adventure choice]
  [:adventure/adventure :adventure/choice => (? :adventure/passage)]
  (some->> (:choice/target choice) (passage adventure)))

(>defn path-steps
  "Given an `adventure` and a `trail` (vector of visited passage ids, the current
   one last), returns a vector of step maps in visit order. Each step is:

     * `:step/passage` - the passage at that point in the trail
     * `:step/choice`  - the choice that led to the next passage, or nil for the
                         last (current) step

   This is the view-model the player uses to render the append-and-scroll trail."
  [adventure trail]
  [:adventure/adventure [:vector :passage/id] => [:vector map?]]
  (mapv (fn [id next-id]
          (let [p (passage adventure id)]
            {:step/passage p
             :step/choice  (when next-id
                             (first (filter #(= next-id (:choice/target %))
                                            (:passage/choices p))))}))
        trail
        (concat (rest trail) [nil])))

;; ---------------------------------------------------------------------------
;; Editing
;; ---------------------------------------------------------------------------

(>defn add-choice
  "Appends `choice` to the passage identified by `passage-id` within `adventure`."
  [adventure passage-id choice]
  [:adventure/adventure :passage/id :adventure/choice => :adventure/adventure]
  (update-in adventure [:adventure/passages passage-id :passage/choices]
             (fnil conj []) choice))

(>defn set-passage-text
  "Replaces the text of passage `passage-id` with `text`."
  [adventure passage-id text]
  [:adventure/adventure :passage/id :passage/text => :adventure/adventure]
  (assoc-in adventure [:adventure/passages passage-id :passage/text] text))

(>defn set-start
  "Sets the start passage of `adventure` to `passage-id`."
  [adventure passage-id]
  [:adventure/adventure :passage/id => :adventure/adventure]
  (assoc adventure :adventure/start passage-id))

(>defn set-choice-target
  "Points the choice at `choice-index` on passage `passage-id` to `target-id`."
  [adventure passage-id choice-index target-id]
  [:adventure/adventure :passage/id :int :choice/target => :adventure/adventure]
  (assoc-in adventure
            [:adventure/passages passage-id :passage/choices choice-index :choice/target]
            target-id))

(>defn remove-passage
  "Removes passage `passage-id` from `adventure`, pruning any choices that
   targeted it. If it was the start, the start is cleared."
  [adventure passage-id]
  [:adventure/adventure :passage/id => :adventure/adventure]
  (let [prune (fn [cs] (filterv #(not= passage-id (:choice/target %)) cs))]
    (-> adventure
        (update :adventure/passages
                (fn [ps] (-> (dissoc ps passage-id)
                             (update-vals (fn [p] (update p :passage/choices prune))))))
        (cond-> (= passage-id (:adventure/start adventure))
          (assoc :adventure/start nil)))))

;; ---------------------------------------------------------------------------
;; Validation
;; ---------------------------------------------------------------------------

(>defn- reachable-ids
  "Returns the set of passage ids reachable from the start of `adventure` by
   following choice targets. Empty when there is no valid start."
  [adventure]
  [:adventure/adventure => [:set :passage/id]]
  (let [start (:adventure/start adventure)
        ps    (:adventure/passages adventure)]
    (if (or (nil? start) (not (contains? ps start)))
      #{}
      (loop [seen #{} stack [start]]
        (if-let [id (peek stack)]
          (if (seen id)
            (recur seen (pop stack))
            (let [targets (->> (get-in ps [id :passage/choices])
                               (keep :choice/target)
                               (filter #(contains? ps %)))]
              (recur (conj seen id) (into (pop stack) targets))))
          seen)))))

(>defn validate
  "Returns a vector of problem maps describing why `adventure` is not yet ready
   to play; an empty vector means it is valid. Each problem carries a
   `:problem/type` and may include `:problem/passage` and `:problem/choice`
   (a choice index) for context. Problem types:

     * `:problem/no-start`       - no start passage is set
     * `:problem/dangling-start` - the start id names no existing passage
     * `:problem/dangling-choice`- a choice has no target, or a missing one
     * `:problem/unreachable`    - a passage cannot be reached from the start"
  [adventure]
  [:adventure/adventure => [:vector map?]]
  (let [start     (:adventure/start adventure)
        ps        (:adventure/passages adventure)
        reachable (reachable-ids adventure)]
    (vec
     (concat
      (when (nil? start)
        [{:problem/type :problem/no-start}])
      (when (and start (not (contains? ps start)))
        [{:problem/type :problem/dangling-start :problem/passage start}])
      (for [[pid p]  ps
            [idx c]  (map-indexed vector (:passage/choices p))
            :let     [t (:choice/target c)]
            :when    (or (nil? t) (not (contains? ps t)))]
        {:problem/type :problem/dangling-choice :problem/passage pid :problem/choice idx})
      (for [pid   (keys ps)
            :when (and (seq reachable) (not (contains? reachable pid)))]
        {:problem/type :problem/unreachable :problem/passage pid})))))

(>defn valid?
  "True when `adventure` has no validation problems (ready to play)."
  [adventure]
  [:adventure/adventure => :boolean]
  (empty? (validate adventure)))
