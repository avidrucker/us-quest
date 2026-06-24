(ns adventure.samples
  "A small, complete built-in adventure used for the demo and as test fixtures.
   It is a multi-level branching tree (converging choices, a follow-up question,
   several warm endings) so it shows off what the app can do. Built entirely
   through the pure domain API. Pure branching — there are no wrong answers.

   This is a generic template; replace it with your own adventure once your
   personal content is ready."
  (:require
   [adventure.domain :as d]))

(defn sample-adventure
  "Returns a fresh copy of the demo adventure (fixed structure, new ids)."
  []
  (let [q-ready  (d/new-passage "Ready for a tiny adventure about us? 💛")
        q-meet   (d/new-passage "Easy one first — where did we first meet?")
        q-moment (d/new-passage "What do you remember most about that day?")
        e-blur   (d/new-passage "That's okay — all that matters is it led to you and me. 💛")
        e-talk   (assoc (d/new-passage "Same. I didn't want it to end — and I still don't. 💛")
                        :passage/image "💛")
        e-nerves (d/new-passage "Caught me. You make me nervous in the very best way. 💛")
        e-vibe   (d/new-passage "The vibe was you. It always is. ✨💛")
        id       :passage/id]
    (-> (d/new-adventure "How well do you know us? 💛")
        (d/add-passage q-ready)        ; first passage → the start
        (d/add-passage q-meet)
        (d/add-passage q-moment)
        (d/add-passage e-blur)
        (d/add-passage e-talk)
        (d/add-passage e-nerves)
        (d/add-passage e-vibe)
        ;; start: two playful choices that converge on the first real question
        (d/add-choice (id q-ready) {:choice/label "Always 🥰"        :choice/target (id q-meet)})
        (d/add-choice (id q-ready) {:choice/label "Do I get a prize? 😏" :choice/target (id q-meet)})
        ;; meet: one branch ends early, one goes deeper
        (d/add-choice (id q-meet)  {:choice/label "Somewhere I'll never forget" :choice/target (id q-moment)})
        (d/add-choice (id q-meet)  {:choice/label "Ahh, it's a happy blur 😅"    :choice/target (id e-blur)})
        ;; moment: three warm endings
        (d/add-choice (id q-moment) {:choice/label "We talked for hours"     :choice/target (id e-talk)})
        (d/add-choice (id q-moment) {:choice/label "How nervous you were 😳" :choice/target (id e-nerves)})
        (d/add-choice (id q-moment) {:choice/label "Honestly… the vibe ✨"    :choice/target (id e-vibe)}))))
