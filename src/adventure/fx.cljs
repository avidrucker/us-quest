(ns adventure.fx
  "re-frame effect and coeffect registrations for localStorage persistence and
   player scrolling. Keeping these out of the event handlers lets the handlers
   stay pure and testable, while the actual I/O lives behind `:store/library`
   (cofx) and `:store/save-library!` (fx)."
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [adventure.share :as share]
   [adventure.storage :as storage]))

(rf/reg-cofx
 :store/library
 (fn [cofx _]
   (assoc cofx :store/library (storage/read-library))))

(rf/reg-fx
 :store/save-library!
 (fn [library]
   (storage/write-library! library)))

(rf/reg-cofx
 :store/seeded
 (fn [cofx _]
   (assoc cofx :store/seeded (storage/read-seeded))))

(rf/reg-fx
 :store/save-seeded!
 (fn [ids]
   (storage/write-seeded! ids)))

(rf/reg-cofx
 :share/incoming
 (fn [cofx _]
   (assoc cofx :share/incoming (share/adventure-from-location))))

(rf/reg-fx
 :share/clear-hash!
 (fn [_]
   (share/clear-hash!)))

(rf/reg-fx
 :share/make-link!
 (fn [adventure]
   (let [url (share/share-url adventure)]
     (some-> (.. js/navigator -clipboard) (.writeText url))
     (rf/dispatch [:adventure.events/share-ready url]))))

;; After a choice appends a passage, scroll the newest (active) passage into
;; view — the "scroll" half of the append-and-scroll trail. Runs after the
;; re-render so the new node exists; a no-op when the element is absent.
(rf/reg-fx
 :scroll/to-active
 (fn [_]
   (r/after-render
    (fn []
      (some-> (.getElementById js/document "active-passage")
              (.scrollIntoView #js {:behavior "smooth" :block "center"}))))))
