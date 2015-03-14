(ns no-sports.util
  (:require [clojure.string :as s]
            [clojure.core.async :as async :refer [<! >! go-loop chan close!]]
            [cheshire.core :as json]
            [clj-tokenizer.core :as tok])
  (:import com.fasterxml.jackson.core.JsonParseException))

(defn mapk
  "Map over keys in a map."
  [f m]
  (->> m
       (map #(vector (f (key %)) (val %)))
       (into {})))

(defn- remove-urls
  [text]
  (s/replace text #"http://\S*" ""))

(defn remove-newlines
  [text]
  (s/replace text #"\n" " "))

(defn tokenize
  [s]
  {:pre [(string? s)]}
  ((comp set
         tok/token-seq
         tok/stemmed
         tok/token-stream-without-stopwords
         remove-urls
         remove-newlines)
   s))

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

(defn- gets
  [m k]
  (if (coll? k)
    (get-in m k)
    (get m k)))

(defn- select
  [m ks]
  (map (partial gets m) ks))

(defn tap
  "Create a transducer that prints the values of the supplied keys
  but does not transform the input.

  (tap \"Got tweet with id %d and body %s\" :id [:body :text])"
  [fmt & ks]
  (fn [xf]
    (fn
      ([] (xf))
      ([result] (xf result))
      ([result input]
       (println (apply format fmt (select input ks)))
       (xf result input)))))
