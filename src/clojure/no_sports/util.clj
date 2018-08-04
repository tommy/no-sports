(ns no-sports.util
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as s]
    [clojure.core.async :as async]
    [clj-tokenizer.core :as tok]
    [clj-http.client :as http]))


;; collection utils

(defn mapk
  "Map over keys in a map."
  [f m]
  (->> m
       (map #(vector (f (key %)) (val %)))
       (into {})))


;; string utils

(defn remove-urls
  [text]
  {:pre [(string? text)]}
  (s/replace text #"https?://\S*" ""))

(defn remove-newlines
  [text]
  {:pre [(string? text)]}
  (s/replace text "\n" " "))

(defn tokenize
  [text]
  {:pre [(string? text)]}
  (-> text
      (remove-newlines)
      (remove-urls)
      (tok/token-stream-without-stopwords)
      (tok/stemmed)
      (tok/token-seq)
      (set)))


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
          (log/info (apply format fmt ((apply juxt fs) input)))
          (log/info fmt))
        (xf result input))))))

(defn report
  [url]
  (fn [xf]
    (fn
      ([] (xf))
      ([result] (xf result))
      ([result input]
       (http/get url)
       (log/infof "Reported to %s" url)
       (xf result input)))))

(defn pipe
  "Create a return a new channel that will receive all messages
  from the from channel but allows specifying a transformer.

  Example:
  (pipe (listen!) 10 (filter retweet?))"
  [from & opts]
  (let [to (apply async/chan opts)]
    (async/pipe from to)
    to))
