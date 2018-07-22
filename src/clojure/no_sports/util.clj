(ns no-sports.util
  (:require [clojure.string :as s]
            [clojure.core.async :as async :refer [<! >! go-loop chan close!]]
            [clojure.tools.logging :refer [info infof debugf debug]]
            [cheshire.core :as json]
            [clj-tokenizer.core :as tok]
            [clj-http.client :as http])
  (:import com.fasterxml.jackson.core.JsonParseException))

;; collection utils

(defn mapk
  "Map over keys in a map."
  [f m]
  (->> m
       (map #(vector (f (key %)) (val %)))
       (into {})))

(defn- gets
  [m k]
  (if (coll? k)
    (get-in m k)
    (get m k)))

(defn- select
  [m ks]
  (map (partial gets m) ks))


;; string utils

(defn remove-urls
  [text]
  (s/replace text #"https?://\S*" ""))

(defn remove-newlines
  [text]
  (s/replace text "\n" " "))

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
      (debugf "Couldn't parse JSON: %s" (.getMessage e)))))

(defn escaped
  [s]
  (let [f #(if-let [c (char-escape-string %)] c %)]
    (reduce str (map f s))))

(def whitespace?
  (partial re-matches #"\s*"))

;; channel

(defn tap
  "Create a transducer that logs the values of the supplied keys
  but does not transform the input.

  (tap \"Got tweet with id %d and body %s\" :id [:body :text])"
  ([message]
   (tap message nil))
  ([fmt fs]
   (fn [xf]
     (fn
       ([] (xf))
       ([result] (xf result))
       ([result input]
        (if (seq fs)
          (info (apply format fmt ((apply juxt fs) input)))
          (info fmt))
        (xf result input))))))

(defn report
  [url]
  (fn [xf]
    (fn
      ([] (xf))
      ([result] (xf result))
      ([result input]
       (http/get url)
       (infof "Reported to %s" url)
       (xf result input)))))

(def whitespace-filter
  (comp (tap "tick")
        (remove whitespace?)))

(defn pipe
  "Create a return a new channel that will receive all messages
  from the from channel but allows specifying a transformer.

  Example:
  (pipe (listen!) 10 (filter retweet?))"
  [from & opts]
  (let [to (apply chan opts)]
    (async/pipe from to)
    to))

(defn parse-json
  "Read (possibly partial) JSON strings from a channel and emit the parsed data
  structures to a returned channel."
  [in]
  (let [out (chan)
        in (pipe in 10 whitespace-filter)]
    (go-loop [acc ""]
      (if-let [v (<! in)]
        (do
          (debug (format "Got a piece of json: '%s'" (escaped v)))
          (debug (format "Accumulator holds: '%s'" (escaped acc)))

          (if-let [parsed (maybe-parse (str acc v))]
            (do (>! out parsed)
                (recur ""))
            (recur (str acc v))))
        (close! out)))
    out))
