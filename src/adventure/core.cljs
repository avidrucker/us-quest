(ns adventure.core
  "App entry point: mounts the Reagent root and initializes app-db."
  (:require
   [reagent.dom.client :as rdomc]
   [re-frame.core :as rf]
   [adventure.events :as events]
   [adventure.subs]
   [adventure.views :as views]))

(defonce root
  (rdomc/create-root (.getElementById js/document "app")))

(defn mount! []
  (rdomc/render root [views/app]))

(defn ^:dev/after-load re-render!
  "Re-mount on hot reload, clearing the subscription cache so subs recompute."
  []
  (rf/clear-subscription-cache!)
  (mount!))

(defn init!
  "Called once on page load (see shadow-cljs.edn :init-fn)."
  []
  (rf/dispatch-sync [::events/initialize-db])
  (mount!))
