(ns adventure.views
  "Reagent components, routed by `:route`. Milestone 3 delivers the library list
   and the append-and-scroll player; the editor arrives in Milestone 5."
  (:require
   [re-frame.core :as rf]
   [adventure.domain :as d]
   [adventure.events :as events]
   [adventure.subs :as subs]))

;; ---------------------------------------------------------------------------
;; Library
;; ---------------------------------------------------------------------------

(defn library-view []
  (let [adventures @(rf/subscribe [::subs/library-list])]
    [:section.library
     [:h1 "Adventures in Us " [:span.heart "💛"]]
     [:p.subtitle "A little choose-your-own-adventure, made for you."]
     [:ul.adventure-list
      (for [adv adventures]
        ^{:key (str (:adventure/id adv))}
        [:li.adventure-card
         [:span.adventure-title (:adventure/title adv)]
         [:button.btn.primary
          {:on-click #(rf/dispatch [::events/start-playthrough (:adventure/id adv)])}
          "Play ▸"]])]]))

;; ---------------------------------------------------------------------------
;; Player
;; ---------------------------------------------------------------------------

(defn passage-image
  "Renders an optional passage image: a short string is treated as emoji/text,
   anything that looks like a URL is rendered as an <img>."
  [image]
  (when (seq image)
    (if (re-find #"^(https?:)?//|\.(png|jpe?g|gif|webp|svg)$" image)
      [:img.passage-image {:src image :alt ""}]
      [:div.passage-image.emoji image])))

(defn step-view
  "One passage in the trail. `active?` marks the current (last) step."
  [{:step/keys [passage choice]} active?]
  [:article.passage {:class (when active? "active")}
   (passage-image (:passage/image passage))
   [:p.passage-text (:passage/text passage)]
   (when choice
     [:p.chosen [:span.chosen-label "❯ " (:choice/label choice)]])])

(defn choices-view [passage]
  [:div.choices
   (for [[idx choice] (map-indexed vector (:passage/choices passage))]
     ^{:key idx}
     [:button.btn.choice
      {:on-click #(rf/dispatch [::events/choose choice])}
      (:choice/label choice)])])

(defn ending-view []
  [:div.ending
   [:p.ending-note "♡ the end ♡"]
   [:button.btn {:on-click #(rf/dispatch [::events/restart])} "Start over"]])

(defn player-view []
  (let [steps     @(rf/subscribe [::subs/path-steps])
        current   @(rf/subscribe [::subs/current-passage])
        at-start? @(rf/subscribe [::subs/at-start?])]
    [:section.player
     [:div.player-bar
      [:button.btn.ghost {:on-click #(rf/dispatch [::events/go-to-library])} "← Library"]
      (when-not at-start?
        [:button.btn.ghost {:on-click #(rf/dispatch [::events/go-back])} "Back"])]
     [:div.trail
      (doall
       (for [[idx step] (map-indexed vector steps)]
         ^{:key idx}
         [step-view step (= idx (dec (count steps)))]))]
     (when current
       (if (d/ending? current)
         [ending-view]
         [choices-view current]))]))

;; ---------------------------------------------------------------------------
;; Root
;; ---------------------------------------------------------------------------

(defn app []
  (let [route @(rf/subscribe [::subs/route])]
    [:main.app
     (case route
       :player [player-view]
       [library-view])]))
