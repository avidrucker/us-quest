(ns adventure.share
  "Encode an adventure into a URL so it can be shared as a tappable link — no
   server, no account. The adventure is printed as EDN, encoded as UTF-8-safe,
   URL-safe base64, and carried in the location hash (#a=...).

   `encode`/`decode` are pure and unit-tested; `share-url`,
   `adventure-from-location`, and `clear-hash!` touch js/window/location.

   (Adventures are small, so we don't compress; if links ever grow too long,
   slot a compressor between `pr-str` and the base64 step.)"
  (:require
   [clojure.string :as str]
   [cljs.reader :as reader]))

(def ^:private param "a")

(defn- utf8->base64 [s]
  (js/btoa (js/unescape (js/encodeURIComponent s))))

(defn- base64->utf8 [b]
  (js/decodeURIComponent (js/escape (js/atob b))))

(defn encode
  "Encodes `adventure` into a URL-safe string."
  [adventure]
  (-> (pr-str adventure)
      utf8->base64
      (str/replace "+" "-")
      (str/replace "/" "_")
      (str/replace "=" "")))

(defn- repad
  "Restores base64 padding to a length that is a multiple of 4."
  [s]
  (case (mod (count s) 4)
    2 (str s "==")
    3 (str s "=")
    s))

(defn decode
  "Decodes a string produced by `encode` back into an adventure, or nil when the
   input is blank or malformed (never throws)."
  [s]
  (when (seq s)
    (try
      (-> s
          (str/replace "-" "+")
          (str/replace "_" "/")
          repad
          base64->utf8
          reader/read-string)
      (catch :default _ nil))))

(defn share-url
  "Returns a full share URL for `adventure` (current origin + path, adventure in
   the hash)."
  [adventure]
  (str (.. js/window -location -origin)
       (.. js/window -location -pathname)
       "#" param "=" (encode adventure)))

(defn adventure-from-location
  "Returns the adventure encoded in the current URL hash (#a=...), or nil."
  []
  (let [hash (.. js/window -location -hash)
        m    (re-find (re-pattern (str param "=([^&]+)")) hash)]
    (when m (decode (second m)))))

(defn clear-hash!
  "Removes the hash from the URL without reloading the page."
  []
  (.replaceState js/history nil "" (.. js/window -location -pathname)))
