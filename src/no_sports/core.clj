(ns no-sports.core
  (:require [clojure.string :as s]))

(defn- remove-urls
  [text]
  (s/replace text #"http://\S*" ""))

(defn remove-newlines
  [text]
  (-> text
      (s/replace #"\n" " ")))

(defn normalize-text
  [text]
  (-> text
      remove-urls
      s/lower-case
      (s/replace #"'" "")
      (s/replace #"\W+" " ")
      (s/replace #"\s+" " ")
      s/trim))

(comment
  (normalize-text "don't turn apsotrophe's into space's")
  (remove-urls "this is a test http://t.co/asdf")
  (normalize-text "this is a test http://t.co/asdf")
  (normalize-text "as,df  ff\nbb")
  (remove-newlines "as,df  ff\nbb"))
