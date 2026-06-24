(ns adventure.samples
  "A small, complete built-in adventure used for the player demo and as test
   fixtures. Built entirely through the pure domain API."
  (:require
   [adventure.domain :as d]))

(defn sample-adventure
  "Returns a small, complete sample adventure. Structure is fixed; ids are
   freshly generated each call. The start passage offers three choices, each
   leading to a warm ending (pure branching — there are no wrong answers)."
  []
  (let [meet    (d/new-passage "Where did we first meet?")
        library (d/new-passage "The quiet corner by the windows. 📚 I pretended to read — really I was just hoping you'd look up. 💛")
        coffee  (d/new-passage "The little coffee shop on the corner. ☕ You ordered something I'd never heard of, and I was smitten before the first sip. 💛")
        party   (d/new-passage "Ha — not quite! 😄 But honestly, anywhere feels like where we met, as long as you're there. 💛")]
    (-> (d/new-adventure "How well do you know us? 💛")
        (d/add-passage meet)        ; first passage becomes the start
        (d/add-passage library)
        (d/add-passage coffee)
        (d/add-passage party)
        (d/add-choice (:passage/id meet)
                      {:choice/label "The library" :choice/target (:passage/id library)})
        (d/add-choice (:passage/id meet)
                      {:choice/label "The coffee shop" :choice/target (:passage/id coffee)})
        (d/add-choice (:passage/id meet)
                      {:choice/label "A house party" :choice/target (:passage/id party)}))))
