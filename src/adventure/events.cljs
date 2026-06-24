(ns adventure.events
  "re-frame event handlers. Each handler is a plain, pure db -> db function
   (registered below) that delegates domain logic to `adventure.domain`, so the
   handlers can be unit-tested directly against a fixture db."
  (:require
   [re-frame.core :as rf]
   [adventure.db :as db]
   [adventure.domain :as d]
   [adventure.samples :as samples]))

;; ---------------------------------------------------------------------------
;; Handlers (pure)
;; ---------------------------------------------------------------------------

(defn initial-db
  "Returns the initial app-db given a `stored-library` (possibly nil/empty). When
   a stored library exists it is used as-is; otherwise the library is seeded with
   the sample adventure."
  [stored-library]
  (if (seq stored-library)
    (assoc db/default-db :library stored-library)
    (let [adv (samples/sample-adventure)]
      (assoc-in db/default-db [:library (:adventure/id adv)] adv))))

(defn put-adventure
  "Stores `adventure` in the library of `db` under its id."
  [db adventure]
  (assoc-in db [:library (:adventure/id adventure)] adventure))

(defn initial-db-with-share
  "Builds the initial db. When an `incoming` adventure arrived via a share link,
   it is added to whatever library the recipient already had (no sample is
   seeded) and starts playing immediately. Otherwise falls back to the normal
   init from `stored-library` (seeding the sample on first run)."
  [stored-library incoming]
  (if incoming
    (-> db/default-db
        (assoc :library (or stored-library {}))
        (assoc-in [:library (:adventure/id incoming)] incoming)
        (assoc :route :player
               :player {:adventure-id (:adventure/id incoming)
                        :trail        [(:adventure/start incoming)]}))
    (initial-db stored-library)))

(defn share-ready
  "Records a freshly-built share URL so the UI can display/copy it."
  [db [_ url]]
  (assoc db :ui/share-url url))

(defn dismiss-share
  "Clears any displayed share URL."
  [db _]
  (dissoc db :ui/share-url))

(defn start-playthrough
  "Begins playing adventure `adventure-id`: routes to the player and starts the
   trail at the adventure's start passage."
  [db [_ adventure-id]]
  (let [adv (get-in db [:library adventure-id])]
    (-> db
        (assoc :route :player)
        (assoc :player {:adventure-id adventure-id
                        :trail        [(:adventure/start adv)]}))))

(defn choose
  "Advances the playthrough by following `choice` to its target passage."
  [db [_ choice]]
  (update-in db [:player :trail] conj (:choice/target choice)))

(defn go-back
  "Steps back to the previous passage in the trail, never past the start."
  [db _]
  (update-in db [:player :trail]
             (fn [trail] (if (> (count trail) 1) (pop trail) trail))))

(defn restart
  "Restarts the current adventure from its start passage."
  [db _]
  (let [adv (get-in db [:library (get-in db [:player :adventure-id])])]
    (assoc-in db [:player :trail] [(:adventure/start adv)])))

(defn go-to-library
  "Returns to the library view."
  [db _]
  (assoc db :route :library))

;; ---------------------------------------------------------------------------
;; Editor handlers (operate on a working copy at [:editor :adventure])
;; ---------------------------------------------------------------------------

(defn start-new-adventure
  "Opens the editor on a fresh, untitled adventure (not yet in the library)."
  [db _]
  (let [adv (d/new-adventure "Untitled adventure")]
    (assoc db :route :editor :editor {:adventure adv :selected nil})))

(defn edit-adventure
  "Opens the editor on a copy of the library adventure `adventure-id`, selecting
   its start passage."
  [db [_ adventure-id]]
  (let [adv (get-in db [:library adventure-id])]
    (assoc db :route :editor :editor {:adventure adv :selected (:adventure/start adv)})))

(defn editor-select-passage [db [_ id]]
  (assoc-in db [:editor :selected] id))

(defn editor-set-title [db [_ title]]
  (update-in db [:editor :adventure] d/set-title title))

(defn editor-add-passage
  "Adds a new (empty) passage to the working copy and selects it."
  [db _]
  (let [p (d/new-passage "")]
    (-> db
        (update-in [:editor :adventure] d/add-passage p)
        (assoc-in [:editor :selected] (:passage/id p)))))

(defn editor-set-passage-text [db [_ id text]]
  (update-in db [:editor :adventure] d/set-passage-text id text))

(defn editor-set-passage-image [db [_ id image]]
  (update-in db [:editor :adventure] d/set-passage-image id image))

(defn editor-set-start [db [_ id]]
  (update-in db [:editor :adventure] d/set-start id))

(defn editor-remove-passage [db [_ id]]
  (-> db
      (update-in [:editor :adventure] d/remove-passage id)
      (update-in [:editor :selected] #(when (not= % id) %))))

(defn editor-add-choice [db [_ passage-id]]
  (update-in db [:editor :adventure] d/add-choice passage-id
             {:choice/label "New choice" :choice/target nil}))

(defn editor-set-choice-label [db [_ passage-id idx label]]
  (update-in db [:editor :adventure] d/set-choice-label passage-id idx label))

(defn editor-set-choice-target
  "Sets the target of choice `idx` on `passage-id`. `target` may be nil (no
   target yet), so this assoc's directly rather than via the domain fn."
  [db [_ passage-id idx target]]
  (assoc-in db [:editor :adventure :adventure/passages passage-id :passage/choices idx :choice/target]
            target))

(defn editor-remove-choice [db [_ passage-id idx]]
  (update-in db [:editor :adventure] d/remove-choice passage-id idx))

(defn commit-working
  "Commits the editor's working copy into the library and clears the editor."
  [db]
  (let [adv (get-in db [:editor :adventure])]
    (-> db
        (assoc-in [:library (:adventure/id adv)] adv)
        (assoc :route :library)
        (assoc :editor {}))))

(defn editor-cancel
  "Discards the working copy and returns to the library."
  [db _]
  (-> db (assoc :route :library) (assoc :editor {})))

;; ---------------------------------------------------------------------------
;; Registrations
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
 ::initialize-db
 [(rf/inject-cofx :store/library)
  (rf/inject-cofx :share/incoming)]
 (fn [{:store/keys [library] :share/keys [incoming]} _]
   (let [db (initial-db-with-share library incoming)]
     (cond-> {:db db :store/save-library! (:library db)}
       incoming (assoc :share/clear-hash! true)))))

(rf/reg-event-fx
 ::save-adventure
 (fn [{:keys [db]} [_ adventure]]
   (let [db' (put-adventure db adventure)]
     {:db                  db'
      :store/save-library! (:library db')})))

(rf/reg-event-db ::start-playthrough start-playthrough)
(rf/reg-event-db ::choose           choose)
(rf/reg-event-db ::go-back          go-back)
(rf/reg-event-db ::restart          restart)
(rf/reg-event-db ::go-to-library    go-to-library)

(rf/reg-event-db ::start-new-adventure    start-new-adventure)
(rf/reg-event-db ::edit-adventure         edit-adventure)
(rf/reg-event-db ::editor-select-passage  editor-select-passage)
(rf/reg-event-db ::editor-set-title       editor-set-title)
(rf/reg-event-db ::editor-add-passage     editor-add-passage)
(rf/reg-event-db ::editor-set-passage-text  editor-set-passage-text)
(rf/reg-event-db ::editor-set-passage-image editor-set-passage-image)
(rf/reg-event-db ::editor-set-start       editor-set-start)
(rf/reg-event-db ::editor-remove-passage  editor-remove-passage)
(rf/reg-event-db ::editor-add-choice      editor-add-choice)
(rf/reg-event-db ::editor-set-choice-label  editor-set-choice-label)
(rf/reg-event-db ::editor-set-choice-target editor-set-choice-target)
(rf/reg-event-db ::editor-remove-choice   editor-remove-choice)
(rf/reg-event-db ::editor-cancel          editor-cancel)

(rf/reg-event-fx
 ::editor-save
 (fn [{:keys [db]} _]
   (let [db' (commit-working db)]
     {:db db' :store/save-library! (:library db')})))

(rf/reg-event-db ::share-ready    share-ready)
(rf/reg-event-db ::dismiss-share  dismiss-share)

(rf/reg-event-fx
 ::share-adventure
 (fn [_ [_ adventure]]
   {:share/make-link! adventure}))
