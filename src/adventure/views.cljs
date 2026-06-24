(ns adventure.views
  "Reagent components, routed by `:route`. Currently a placeholder; the player,
   library, and editor views arrive in later milestones."
  (:require
   [re-frame.core :as rf]
   [adventure.subs :as subs]))

(defn app
  "Root component."
  []
  (let [route @(rf/subscribe [::subs/route])]
    [:main.app
     [:h1 "Adventures in Us 💛"]
     [:p "A little adventure, made for you — coming together one passage at a time."]
     [:p {:style {:opacity 0.5 :font-size "0.85rem"}}
      (str "route: " route)]]))
