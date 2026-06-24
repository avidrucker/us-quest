(ns adventure.samples
  "A small, complete built-in adventure used for the demo and as test fixtures.
   It is a multi-level branching tree (converging choices, a follow-up question,
   several warm endings) so it shows off what the app can do. Built entirely
   through the pure domain API. Pure branching — there are no wrong answers.

   This is a generic template; replace it with your own adventure once your
   personal content is ready."
  (:require
   [adventure.domain :as d]))

;; Stable adventure ids for the built-in demos. These are deterministic so a
;; demo has the same `:adventure/id` across sessions — the key that lets
;; `events/seed-or-merge` add a *missing* demo to a returning user's library
;; without duplicating ones they already have. (Passage ids stay random per
;; build; the merge key is the adventure id.)
(def sample-adventure-id   #uuid "115a3e1d-0000-4000-8000-000000000001")
(def cogbias-adventure-id  #uuid "115a3e1d-0000-4000-8000-000000000002")
(def japanese-adventure-id #uuid "115a3e1d-0000-4000-8000-000000000003")
(def dungeon-adventure-id  #uuid "115a3e1d-0000-4000-8000-000000000004")

(defn sample-adventure
  "Returns a fresh copy of the demo adventure (fixed structure + stable id)."
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
    (-> (assoc (d/new-adventure "How well do you know us? 💛") :adventure/id sample-adventure-id)
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
    (-> (assoc (d/new-adventure "Adventures in Cognitive Biases (intro) 🧠") :adventure/id cogbias-adventure-id)
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

(defn japanese-intro-adventure
  "Returns a fresh copy of a beginner Japanese-greeting adventure: a first-time
   meeting at a party where the player learns three phrases in order —
   はじめまして (hajimemashite), ～です (~ desu, \"I'm ~.\"), and よろしくおねがいします
   (yoroshiku onegai shimasu). Each question offers the correct phrase plus
   plausible-but-wrong ones; a wrong choice routes to a short, in-character
   reaction from the NPC (あいこ / Aiko) that loops back so the player can retry.
   Built entirely through the pure domain API; the only ending is the success
   payoff."
  []
  (let [;; questions
        q-greet (assoc (d/new-passage "🎉 You're at a party. A friendly woman bows and smiles: 「はじめまして！」 (hajimemashite — \"nice to meet you\"). How do you reply?")
                       :passage/image "🎎")
        q-name  (d/new-passage "She brightens: 「わたしは あいこ です。おなまえ は なん です か？」 (watashi wa Aiko desu, onamae wa nan desu ka — \"I'm Aiko. What's your name?\") Introduce yourself — \"I'm ___.\"")
        q-close (d/new-passage "あいこ smiles warmly and waits. Wrap up the introduction the polite way.")
        ;; reactions to wrong answers — each loops back to its question
        r-arigatou (d/new-passage "She tilts her head, amused: \"Thank you…? 😅\" — ありがとう (arigatou) means \"thank you,\" not a greeting.")
        r-sumimasen (d/new-passage "She giggles: \"No need to apologize! 😄\" — すみません (sumimasen) means \"excuse me / sorry.\"")
        r-konbanwa (d/new-passage "\"Good evening to you too! 🌙\" she laughs — こんばんは (konbanwa) is a greeting, but she asked your name.")
        r-sayounara (d/new-passage "She blinks, then laughs: \"Goodbye already?! 😂 You just got here!\" — さようなら (sayounara) means \"goodbye.\"")
        r-thanks2 (d/new-passage "\"Close in spirit! 💬\" — ありがとう (arigatou) is \"thank you.\" To finish an introduction politely, there's a set phrase.")
        ;; the one ending: success
        e-done  (assoc (d/new-passage "あいこ beams and bows: 「よろしく おねがいします！」 🎉 You made your first introduction in Japanese — はじめまして, ～です, and よろしくおねがいします. Yatta! 🌸")
                       :passage/image "🌸")
        id      :passage/id
        ;; a "try again" loop: `from`'s single choice points back to `to`
        retry (fn [adv from to] (d/add-choice adv (id from) {:choice/label "↩ Try again" :choice/target (id to)}))]
    (-> (assoc (d/new-adventure "Hajimemashite! A first meeting in Japanese 🇯🇵") :adventure/id japanese-adventure-id)
        (d/add-passage q-greet)        ; first passage → the start
        (d/add-passage q-name)
        (d/add-passage q-close)
        (d/add-passage r-arigatou)
        (d/add-passage r-sumimasen)
        (d/add-passage r-konbanwa)
        (d/add-passage r-sayounara)
        (d/add-passage r-thanks2)
        (d/add-passage e-done)         ; no choices → the (only) ending
        ;; greeting: one correct, two wrong (each loops back)
        (d/add-choice (id q-greet) {:choice/label "はじめまして (hajimemashite) — nice to meet you" :choice/target (id q-name)})
        (d/add-choice (id q-greet) {:choice/label "ありがとう (arigatou) — thank you"               :choice/target (id r-arigatou)})
        (d/add-choice (id q-greet) {:choice/label "すみません (sumimasen) — excuse me / sorry"        :choice/target (id r-sumimasen)})
        ;; self-introduction: one correct, two wrong
        (d/add-choice (id q-name) {:choice/label "（なまえ）です ( ___ desu) — \"I'm ___.\"" :choice/target (id q-close)})
        (d/add-choice (id q-name) {:choice/label "こんばんは (konbanwa) — good evening"      :choice/target (id r-konbanwa)})
        (d/add-choice (id q-name) {:choice/label "さようなら (sayounara) — goodbye"          :choice/target (id r-sayounara)})
        ;; closing: one correct, one wrong
        (d/add-choice (id q-close) {:choice/label "よろしく おねがいします (yoroshiku onegai shimasu) — pleased to meet you" :choice/target (id e-done)})
        (d/add-choice (id q-close) {:choice/label "ありがとう (arigatou) — thank you"                                    :choice/target (id r-thanks2)})
        ;; wrong-answer reactions loop back to their question
        (retry r-arigatou  q-greet)
        (retry r-sumimasen q-greet)
        (retry r-konbanwa  q-name)
        (retry r-sayounara q-name)
        (retry r-thanks2   q-close))))

(defn dungeon-crawl-adventure
  "Returns a fresh copy of a pure choice-driven D&D dungeon crawl — the first
   demo with stakes: win / lose / secret endings, path convergence, and a few
   laughs. There is no state; the treasure path is encoded structurally (a
   separate `cavern-rich` passage), not via variables."
  []
  (let [;; rooms
        entrance (assoc (d/new-passage "🔥 Torchlight licks the mouth of the Cracked Crypt. A heavy oak door stands ahead; a narrow crack splits the wall to your left.") :passage/image "🔥")
        goblin   (assoc (d/new-passage "👹 Beyond the door a goblin gnaws a bone — and spots you. A rusty sword leans within reach.") :passage/image "👹")
        fightw   (assoc (d/new-passage "🗝️ You snatch the blade and lunge! The goblin shrieks and flees, dropping an iron key. Ahead: a squat wooden chest and a passage into the dark.") :passage/image "🗝️")
        gotgold  (assoc (d/new-passage "💰 Your blade taps the lid — just wood. Inside, a hoard of gold coins! You stuff your pockets and creep onward.") :passage/image "💰")
        tunnel   (d/new-passage "You wriggle through the crack and drop onto a high ledge — overlooking a vast cavern below.")
        cavern   (assoc (d/new-passage "🐉 A great dragon sleeps atop a mountain of gold. The exit tunnel beckons across the cavern.") :passage/image "🐉")
        cavernr  (assoc (d/new-passage "🐉 The same sleeping dragon, the same glittering hoard — but your pockets already jingle with stolen gold. The exit is so close.") :passage/image "🐉")
        wakes    (assoc (d/new-passage "🔥 The dragon's eye snaps open. It rears back, throat glowing!") :passage/image "🔥")
        talk     (d/new-passage "The dragon blinks, head tilting. \"Few visitors bother with hello,\" it rumbles, almost pleased.")
        ;; endings
        e-chicken (assoc (d/new-passage "🍺 You decide glory can wait, and head back to the tavern for a warm ale. Live to crawl another day! (the end)") :passage/image "🍺")
        e-mimic   (assoc (d/new-passage "💀 The chest sprouts teeth and CHOMPS. A mimic — classic. Adventuring is hard. (the end)") :passage/image "💀")
        e-triumph (assoc (d/new-passage "🏆 You slip into daylight with a fortune and your skin intact. Bards will sing of this. (the end)") :passage/image "🏆")
        e-escape  (assoc (d/new-passage "😮‍💨 You burst out into the sun, heart hammering — mostly empty-handed, gloriously alive. (the end)") :passage/image "😮‍💨")
        e-roasted (assoc (d/new-passage "🔥 You raise your blade against a dragon. Bold. Brief. Extra crispy. 💀 (the end)") :passage/image "🔥")
        e-friend  (assoc (d/new-passage "🐉💛 You and the dragon trade tales until dawn. It presses a single coin into your hand — \"for the road, friend.\" The rarest ending of all. (the end)") :passage/image "🐉")
        id   :passage/id
        step (fn [adv from to label] (d/add-choice adv (id from) {:choice/label label :choice/target (id to)}))]
    (-> (assoc (d/new-adventure "Into the Cracked Crypt 🐉") :adventure/id dungeon-adventure-id)
        (d/add-passage entrance)        ; first → the start
        (d/add-passage goblin)
        (d/add-passage fightw)
        (d/add-passage gotgold)
        (d/add-passage tunnel)
        (d/add-passage cavern)
        (d/add-passage cavernr)
        (d/add-passage wakes)
        (d/add-passage talk)
        (d/add-passage e-chicken)
        (d/add-passage e-mimic)
        (d/add-passage e-triumph)
        (d/add-passage e-escape)
        (d/add-passage e-roasted)
        (d/add-passage e-friend)
        ;; entrance
        (step entrance goblin    "Shoulder open the oak door")
        (step entrance tunnel    "Squeeze through the crack")
        (step entrance e-chicken "Actually… back to the tavern 🍺")
        ;; goblin
        (step goblin fightw "Grab the rusty sword and fight")
        (step goblin cavern "Sneak past in the shadows")
        (step goblin cavern "Offer it your trail rations")
        ;; chest after the fight
        (step fightw e-mimic "Fling the chest open")
        (step fightw gotgold "Tap it with your blade first")
        (step fightw cavern  "Ignore it and press on")
        ;; got-gold → the "rich" cavern (paths to treasure endings)
        (step gotgold cavernr "Onward, pockets heavy")
        ;; the crack drops you into the same cavern (convergence)
        (step tunnel cavern "Drop down into the cavern")
        ;; cavern (no gold yet)
        (step cavern wakes    "Snatch a handful of gold")
        (step cavern e-escape "Tiptoe to the exit")
        (step cavern talk     "Clear your throat and say hello")
        ;; cavern-rich (already carrying gold)
        (step cavernr e-triumph "Quit while ahead — tiptoe out")
        (step cavernr wakes     "Greed! grab even more")
        (step cavernr talk      "Say hello to the dragon")
        ;; dragon wakes
        (step wakes e-escape  "Sprint for the exit")
        (step wakes e-roasted "Stand and fight")
        (step wakes e-escape  "Throw the gold and run")
        ;; dragon talk
        (step talk e-friend "Ask to share a story")
        (step talk e-escape "Lose your nerve and bolt"))))

(defn built-in-adventures
  "Returns fresh copies of all built-in demo adventures (each with its stable id)."
  []
  [(sample-adventure)
   (cogbias-intro-adventure)
   (japanese-intro-adventure)
   (dungeon-crawl-adventure)])

(defn built-in-titles
  "Set of the built-in demo titles. Used to retire legacy random-id auto-seeds
   from a pre-stable-id library during the one-time migration."
  []
  (into #{} (map :adventure/title) (built-in-adventures)))
