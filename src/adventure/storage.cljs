(ns adventure.storage
  "Persistence of the adventure library to browser localStorage. Serialization
   (`serialize`/`deserialize`) is pure and unit-tested; `read-library`/
   `write-library!` perform the actual js/localStorage I/O."
  (:require
   [cljs.reader :as reader]))

(def ^:private storage-key "us-quest/library")
(def ^:private seeded-key  "us-quest/seeded-demos")

(defn serialize
  "Returns an EDN string for `library` (a map of id -> adventure)."
  [library]
  (pr-str library))

(defn deserialize
  "Parses an EDN `string` produced by `serialize` back into a library, or nil
   when `string` is blank/nil."
  [string]
  (when (seq string)
    (reader/read-string string)))

(defn read-library
  "Reads and parses the stored library from localStorage, or nil if absent."
  []
  (deserialize (.getItem js/localStorage storage-key)))

(defn write-library!
  "Writes `library` to localStorage."
  [library]
  (.setItem js/localStorage storage-key (serialize library)))

(defn read-seeded
  "Returns the set of built-in demo ids ever seeded, or nil if the marker is
   absent (i.e. a library that predates stable-id seeding)."
  []
  (deserialize (.getItem js/localStorage seeded-key)))

(defn write-seeded!
  "Writes the set of seeded built-in demo `ids` to localStorage."
  [ids]
  (.setItem js/localStorage seeded-key (serialize ids)))
