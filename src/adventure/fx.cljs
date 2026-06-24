(ns adventure.fx
  "re-frame effect and coeffect registrations for localStorage persistence.
   Keeping these out of the event handlers lets the handlers stay pure and
   testable, while the actual I/O lives behind `:store/library` (cofx) and
   `:store/save-library!` (fx)."
  (:require
   [re-frame.core :as rf]
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
