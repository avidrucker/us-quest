(ns adventure.subs
  "re-frame subscriptions: the UI's read-only views into app-db."
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::route
 (fn [db _]
   (:route db)))
