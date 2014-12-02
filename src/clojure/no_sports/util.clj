(ns no-sports.util
  (:require [clojure.string :as s]
            [clojure.core.async :as async :refer [<! >! go-loop chan close!]]
            [cheshire.core :as json]
            [clj-tokenizer.core :as tok])
  (:import com.fasterxml.jackson.core.JsonParseException))

(defn- remove-urls
  [text]
  (s/replace text #"http://\S*" ""))

(defn remove-newlines
  [text]
  (s/replace text #"\n" " "))

(def tokenize
  (comp tok/token-seq
        tok/stemmed
        tok/token-stream-without-stopwords
        remove-urls))

(defn maybe-parse
  "Tries to parse a string as JSON. Returns nil instead of throwing an
  exception if the string is not valid JSON."
  [s]
  (try
    (json/parse-string s true)
    (catch JsonParseException e
      nil)))

(defn parse-json
  "Read (possibly partial) JSON strings from a channel and emit the parsed data
  structures to a returned channel."
  [in]
  (let [out (chan)]
    (go-loop [acc ""]
      (if-let [v (<! in)]
        (if-let [parsed (maybe-parse (str acc v))]
          (do (>! out parsed)
              (recur ""))
          (recur (str acc v)))
        (close! out)))
    out))

(defn pipe
  "Create a return a new channel that will receive all messages
  from the from channel but allows specifying a transformer.
  
  Example:
  (pipe (listen!) 10 (filter retweet?))"
  [from & opts]
  (let [to (apply chan opts)]
    (async/pipe from to)
    to))

(comment
  (tokenize "don't turn apsotrophe's into space's 10 to 20")
  (remove-urls "this is a test http://t.co/asdf")
  (tokenize "this is a test http://t.co/asdf")
  (tokenize "as,df  ff\nbb")
  (tokenize "this is &amp; an ampersand")
  (remove-newlines "as,df  ff\nbb"))
