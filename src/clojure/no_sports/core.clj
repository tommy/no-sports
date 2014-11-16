(ns no-sports.core
  (:require [clojure.string :as s]
            [clj-tokenizer.core :as tok]))

(defn- remove-urls
  [text]
  (s/replace text #"http://\S*" ""))

(defn remove-newlines
  [text]
  (-> text
      (s/replace #"\n" " ")))

(def tokenize
  (comp tok/token-seq
        tok/stemmed
        tok/token-stream-without-stopwords
        remove-urls))

(comment
  (tokenize "don't turn apsotrophe's into space's 10 to 20")
  (remove-urls "this is a test http://t.co/asdf")
  (tokenize "this is a test http://t.co/asdf")
  (tokenize "as,df  ff\nbb")
  (tokenize "this is &amp; an ampersand")
  (remove-newlines "as,df  ff\nbb"))
