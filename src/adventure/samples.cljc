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

(defn cogbias-intro-adventure
  "Returns a fresh copy of a mostly-linear, story-driven demo adventure that
   recreates the opening ~10 beats of Cassandra Xia's \"Adventures in Cognitive
   Biases\" (https://cassandraxia.com/cogbiases/). Content is paraphrased, with
   in-character buttons (\"Knock\", \"13?!?!\", \"Oh dear\") — a contrast to the
   branchy `sample-adventure`. Pure branching; the last passage is an ending."
  []
  (let [p1  (d/new-passage "You find yourself standing before an old stone monastery, the storm finally easing.")
        p2  (d/new-passage "A curious sign hangs on the door: \"Come, rid yourself of cognitive biases — the moments when even clever minds choose poorly.\"")
        p3  (assoc (d/new-passage "A monk opens the door and greets you with a knowing smile.") :passage/image "🧙")
        p4  (d/new-passage "MONK: I've searched a long while for a successor. The prophecy said today was the last day we could meet.")
        p5  (d/new-passage "MONK: The world is in danger. Through the bystander effect, we've watched from the sidelines as a few tip everything out of balance.")
        p6  (d/new-passage "MONK: The prophecy says my successor must shed some thirteen biases — and change how others see the world.")
        p7  (d/new-passage "MONK: Honestly, it first guessed eighty-eight. I talked it down to thirteen, or I'd never have found anyone. But first, our own minds.")
        p8  (d/new-passage "The first is so common it isn't even formally named: the Magic Answer Fallacy — the urge to answer every question with one tidy, certain number.")
        p9  (d/new-passage "MONK: How many days in a year? \"365,\" most reply. But it's closer to 365.2422 — which is why we have leap years, and even those don't quite settle it.")
        p10 (assoc (d/new-passage "MONK: The first step is humility — most questions have no single magic answer. Hold them as ranges, not points.  (to be continued… ✨)") :passage/image "✨")
        id  :passage/id
        ;; link `from` to `to` with a single labelled choice (a linear step)
        step (fn [adv from to label] (d/add-choice adv (id from) {:choice/label label :choice/target (id to)}))]
    (-> (d/new-adventure "Adventures in Cognitive Biases (intro) 🧠")
        (d/add-passage p1)             ; first passage → the start
        (d/add-passage p2)
        (d/add-passage p3)
        (d/add-passage p4)
        (d/add-passage p5)
        (d/add-passage p6)
        (d/add-passage p7)
        (d/add-passage p8)
        (d/add-passage p9)
        (d/add-passage p10)            ; no choices → the ending
        (step p1 p2  "Approach")
        (step p2 p3  "Knock")
        (step p3 p4  "Next")
        (step p4 p5  "Next")
        (step p5 p6  "Go on")
        (step p6 p7  "13?!?!")
        (step p7 p8  "Oh dear")
        (step p8 p9  "Go on")
        (step p9 p10 "Huh."))))
