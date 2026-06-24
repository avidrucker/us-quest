(ns adventure.subs
  "re-frame subscriptions: the UI's read-only, derived views into app-db.
   Derivations delegate to the pure `adventure.domain`."
  (:require
   [re-frame.core :as rf]
   [adventure.domain :as d]))

(rf/reg-sub ::route   (fn [db _] (:route db)))
(rf/reg-sub ::library (fn [db _] (:library db)))
(rf/reg-sub ::trail   (fn [db _] (get-in db [:player :trail])))

(rf/reg-sub
 ::library-list
 :<- [::library]
 (fn [library _]
   (sort-by :adventure/title (vals library))))

(rf/reg-sub
 ::current-adventure
 (fn [db _]
   (get-in db [:library (get-in db [:player :adventure-id])])))

(rf/reg-sub
 ::path-steps
 :<- [::current-adventure]
 :<- [::trail]
 (fn [[adv trail] _]
   (when (and adv (seq trail))
     (d/path-steps adv (vec trail)))))

(rf/reg-sub
 ::current-passage
 :<- [::current-adventure]
 :<- [::trail]
 (fn [[adv trail] _]
   (when (and adv (seq trail))
     (d/passage adv (last trail)))))

(rf/reg-sub
 ::at-start?
 :<- [::trail]
 (fn [trail _]
   (<= (count trail) 1)))

;; --- Editor ---------------------------------------------------------------

(rf/reg-sub ::editor-adventure (fn [db _] (get-in db [:editor :adventure])))
(rf/reg-sub ::editor-selected  (fn [db _] (get-in db [:editor :selected])))

(rf/reg-sub
 ::editor-passages
 :<- [::editor-adventure]
 (fn [adv _]
   (when adv (vec (vals (:adventure/passages adv))))))

(rf/reg-sub
 ::editor-selected-passage
 :<- [::editor-adventure]
 :<- [::editor-selected]
 (fn [[adv selected] _]
   (when (and adv selected) (d/passage adv selected))))

(rf/reg-sub
 ::editor-problems
 :<- [::editor-adventure]
 (fn [adv _]
   (when adv (d/validate adv))))
