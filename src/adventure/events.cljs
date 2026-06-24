(ns adventure.events
  "re-frame event handlers. Handlers stay thin and delegate domain logic to
   `adventure.domain` (added in Milestone 2)."
  (:require
   [re-frame.core :as rf]
   [adventure.db :as db]))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
