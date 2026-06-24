(ns adventure.storage
  "Persistence of the adventure library to browser localStorage. Serialization
   (`serialize`/`deserialize`) is pure and unit-tested; `read-library`/
   `write-library!` perform the actual js/localStorage I/O."
  (:require
   [cljs.reader :as reader]))

(def ^:private storage-key "us-quest/library")

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
