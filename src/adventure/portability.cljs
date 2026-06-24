(ns adventure.portability
  "Export an adventure to an EDN string (for a downloadable file) and parse one
   back from a file's text. Pure and unit-tested; the actual download / file-read
   I/O lives in `adventure.fx`.

   EDN is the canonical format: it round-trips uuids (`#uuid`) and namespaced
   keywords losslessly — the same shape `adventure.storage` and `adventure.share`
   already persist."
  (:require
   [clojure.string :as str]
   [cljs.reader :as reader]))

(defn export-edn
  "Returns an EDN string for `adventure` (round-trips via `parse-imported`)."
  [adventure]
  (pr-str adventure))

(defn filename
  "A safe download filename for `adventure`: a slug of its title + \".edn\"."
  [adventure]
  (let [slug (-> (or (:adventure/title adventure) "")
                 str/lower-case
                 (str/replace #"[^a-z0-9]+" "-")
                 (str/replace #"^-+|-+$" ""))]
    (str (if (seq slug) slug "adventure") ".edn")))

(defn- choice-ok? [c]
  (and (map? c) (string? (:choice/label c))))

(defn- passage-ok? [p]
  (and (map? p)
       (uuid? (:passage/id p))
       (string? (:passage/text p))
       (vector? (:passage/choices p))
       (every? choice-ok? (:passage/choices p))))

(defn adventure-shaped?
  "True when `x` is structurally a us-quest adventure the app can load safely.
   Deliberately does NOT require play-validity — a half-finished prototype is
   still importable and can be finished in the editor."
  [x]
  (and (map? x)
       (uuid? (:adventure/id x))
       (string? (:adventure/title x))
       (map? (:adventure/passages x))
       (every? passage-ok? (vals (:adventure/passages x)))
       (let [start (:adventure/start x)] (or (nil? start) (uuid? start)))))

(defn parse-imported
  "Parses `text` (an exported `.edn` file's contents) into `{:ok adventure}` or
   `{:error message}`. Never throws — malformed EDN and non-adventure data both
   return an `:error`."
  [text]
  (let [data (try (reader/read-string text) (catch :default _ ::unreadable))]
    (cond
      (= data ::unreadable)          {:error "That file isn't readable EDN."}
      (not (adventure-shaped? data)) {:error "That file isn't a us-quest adventure."}
      :else                          {:ok data})))
