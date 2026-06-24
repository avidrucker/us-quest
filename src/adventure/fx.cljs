(ns adventure.fx
  "re-frame effect and coeffect registrations for localStorage persistence.
   Keeping these out of the event handlers lets the handlers stay pure and
   testable, while the actual I/O lives behind `:store/library` (cofx) and
   `:store/save-library!` (fx)."
  (:require
   [re-frame.core :as rf]
   [adventure.storage :as storage]))

(rf/reg-cofx
 :store/library
 (fn [cofx _]
   (assoc cofx :store/library (storage/read-library))))

(rf/reg-fx
 :store/save-library!
 (fn [library]
   (storage/write-library! library)))
