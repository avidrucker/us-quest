(ns adventure.views
  "Reagent components, routed by `:route`: the library, the append-and-scroll
   player, and the form-based authoring editor."
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [adventure.domain :as d]
   [adventure.events :as events]
   [adventure.subs :as subs]))

(defn- target-value [e] (.. e -target -value))

(defn- passage-label
  "A short, human-friendly label for a passage (for lists and dropdowns)."
  [p]
  (let [t (some-> (:passage/text p) str/trim)]
    (if (seq t)
      (if (> (count t) 28) (str (subs t 0 28) "…") t)
      "(empty passage)")))

;; ---------------------------------------------------------------------------
;; Library
;; ---------------------------------------------------------------------------

(defn share-banner []
  (when-let [url @(rf/subscribe [::subs/share-url])]
    [:div.share-banner
     [:span.share-msg "Link copied! 💛 Send it to her:"]
     [:input.share-url {:read-only true :value url
                        :on-focus #(.select (.-target %))}]
     [:button.btn.ghost {:on-click #(rf/dispatch [::events/dismiss-share])} "✕"]]))

(defn library-view []
  (let [adventures @(rf/subscribe [::subs/library-list])]
    [:section.library
     [:h1 "Adventures in Us " [:span.heart "💛"]]
     [:p.subtitle "A little choose-your-own-adventure, made for you."]
     [share-banner]
     [:button.btn.primary.new-btn
      {:on-click #(rf/dispatch [::events/start-new-adventure])} "+ New adventure"]
     [:ul.adventure-list
      (for [adv adventures]
        ^{:key (str (:adventure/id adv))}
        [:li.adventure-card
         [:span.adventure-title (:adventure/title adv)]
         [:div.card-actions
          [:button.btn.ghost
           {:on-click #(rf/dispatch [::events/edit-adventure (:adventure/id adv)])} "Edit"]
          [:button.btn.ghost
           {:on-click #(rf/dispatch [::events/share-adventure adv])} "Share"]
          [:button.btn.primary
           {:on-click #(rf/dispatch [::events/start-playthrough (:adventure/id adv)])} "Play ▸"]]])]]))

;; ---------------------------------------------------------------------------
;; Player
;; ---------------------------------------------------------------------------

(defn passage-image
  "Renders an optional passage image: a URL becomes an <img>, anything else
   (e.g. an emoji) renders as text."
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
;; Editor
;; ---------------------------------------------------------------------------

(defn- problem-text [{:problem/keys [type]}]
  (case type
    :problem/no-start        "No start passage is set."
    :problem/dangling-start  "The start passage no longer exists."
    :problem/dangling-choice "A choice points nowhere — give it a target."
    :problem/unreachable     "A passage can't be reached from the start."
    (str type)))

(defn validation-panel [problems]
  (if (empty? problems)
    [:div.validation.ok "✓ Ready to play"]
    [:div.validation.warn
     [:span.field-label "To fix before playing:"]
     [:ul (for [[i p] (map-indexed vector problems)]
            ^{:key i} [:li (problem-text p)])]]))

(defn target-dropdown [adv passage idx choice]
  (let [passages (vals (:adventure/passages adv))]
    [:select.target-select
     {:value     (str (:choice/target choice))
      :on-change (fn [e]
                   (let [v      (target-value e)
                         target (some #(when (= (str (:passage/id %)) v) (:passage/id %)) passages)]
                     (rf/dispatch [::events/editor-set-choice-target (:passage/id passage) idx target])))}
     [:option {:value ""} "— choose target —"]
     (for [p passages]
       ^{:key (str (:passage/id p))}
       [:option {:value (str (:passage/id p))} (passage-label p)])]))

(defn choice-editor [adv passage idx choice]
  [:div.choice-row
   [:input.choice-label
    {:value       (:choice/label choice)
     :placeholder "Choice label"
     :on-change   #(rf/dispatch [::events/editor-set-choice-label (:passage/id passage) idx (target-value %)])}]
   [target-dropdown adv passage idx choice]
   [:button.btn.ghost.danger
    {:title "Remove choice"
     :on-click #(rf/dispatch [::events/editor-remove-choice (:passage/id passage) idx])} "✕"]])

(defn passage-editor [adv passage start?]
  [:div.passage-editor
   [:label.field
    [:span.field-label "Passage text"]
    [:textarea
     {:value       (:passage/text passage)
      :placeholder "What happens here…"
      :rows        3
      :on-change   #(rf/dispatch [::events/editor-set-passage-text (:passage/id passage) (target-value %)])}]]
   [:label.field
    [:span.field-label "Image — emoji or URL (optional)"]
    [:input
     {:value     (or (:passage/image passage) "")
      :on-change #(rf/dispatch [::events/editor-set-passage-image (:passage/id passage) (target-value %)])}]]
   [:div.choices-editor
    [:span.field-label "Choices"]
    (for [[idx choice] (map-indexed vector (:passage/choices passage))]
      ^{:key idx} [choice-editor adv passage idx choice])
    [:button.btn.ghost
     {:on-click #(rf/dispatch [::events/editor-add-choice (:passage/id passage)])} "+ Add choice"]
    [:p.hint "No choices = this passage is an ending. 💛"]]
   [:div.passage-actions
    (if start?
      [:span.badge "★ Start"]
      [:button.btn.ghost
       {:on-click #(rf/dispatch [::events/editor-set-start (:passage/id passage)])} "Set as start"])
    [:button.btn.ghost.danger
     {:on-click #(rf/dispatch [::events/editor-remove-passage (:passage/id passage)])} "Delete passage"]]])

(defn passage-list [adv passages selected]
  [:div.passage-list
   (for [p passages]
     ^{:key (str (:passage/id p))}
     [:button.passage-chip
      {:class    (when (= (:passage/id p) selected) "selected")
       :on-click #(rf/dispatch [::events/editor-select-passage (:passage/id p)])}
      (when (= (:passage/id p) (:adventure/start adv)) "★ ")
      (passage-label p)])
   [:button.btn.ghost.add-passage
    {:on-click #(rf/dispatch [::events/editor-add-passage])} "+ Add passage"]])

(defn editor-view []
  (let [adv      @(rf/subscribe [::subs/editor-adventure])
        passages @(rf/subscribe [::subs/editor-passages])
        selected @(rf/subscribe [::subs/editor-selected])
        sel-pass @(rf/subscribe [::subs/editor-selected-passage])
        problems @(rf/subscribe [::subs/editor-problems])]
    [:section.editor
     [:div.editor-bar
      [:button.btn.ghost {:on-click #(rf/dispatch [::events/editor-cancel])} "← Cancel"]
      [:button.btn.primary {:on-click #(rf/dispatch [::events/editor-save])} "Save 💾"]]
     [:input.title-input
      {:value       (:adventure/title adv)
       :placeholder "Adventure title"
       :on-change   #(rf/dispatch [::events/editor-set-title (target-value %)])}]
     [validation-panel problems]
     [passage-list adv passages selected]
     (when sel-pass
       [passage-editor adv sel-pass (= (:passage/id sel-pass) (:adventure/start adv))])]))

;; ---------------------------------------------------------------------------
;; Root
;; ---------------------------------------------------------------------------

(defn app []
  (let [route @(rf/subscribe [::subs/route])]
    [:main.app
     (case route
       :player [player-view]
       :editor [editor-view]
       [library-view])]))
