(ns adventure.events
  "re-frame event handlers. Each handler is a plain, pure db -> db function
   (registered below) that delegates domain logic to `adventure.domain`, so the
   handlers can be unit-tested directly against a fixture db."
  (:require
   [re-frame.core :as rf]
   [adventure.db :as db]
   [adventure.samples :as samples]))

;; ---------------------------------------------------------------------------
;; Handlers (pure)
;; ---------------------------------------------------------------------------

(defn initialize-db
  "Builds the initial app-db, seeding the library with the sample adventure."
  [_ _]
  (let [adv (samples/sample-adventure)]
    (assoc-in db/default-db [:library (:adventure/id adv)] adv)))

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
;; Registrations
;; ---------------------------------------------------------------------------

(rf/reg-event-db ::initialize-db    initialize-db)
(rf/reg-event-db ::start-playthrough start-playthrough)
(rf/reg-event-db ::choose           choose)
(rf/reg-event-db ::go-back          go-back)
(rf/reg-event-db ::restart          restart)
(rf/reg-event-db ::go-to-library    go-to-library)
