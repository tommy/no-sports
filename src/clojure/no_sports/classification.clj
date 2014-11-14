(ns no-sports.classification
  (:require [no-sports.export :refer [load-dataset
                                      el]]
            ;[clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [nuroko.lab.core :as nk])
  (:import [no_sports.coders.SetCoder]))

(defn- token-set
  "Given a row from the dataset, returns a set of all tokens of the tweet text."
  [row]
  (as-> row ?
    (el :text ?)
    (s/split ? #"\s+")
    (into #{} ?)))

(defn- all-tokens
  "Returns a set of all tokens in the entire dataset"
  [dataset]
  (reduce
    (fn [acc row] (into acc (token-set row)))
    (sorted-set)
    dataset))

#_(defn- token-coder
  [dataset]
  (let [all (all-tokens dataset)]
    (nk/class-coder :values all)))

(defn- token-set-coder
  [dataset]
  (let [all (all-tokens dataset)]
    (no_sports.coders.SetCoder.
      ^java.util.Collection (into [] all))))

(comment
  (def d (load-dataset))
  (def c (token-set-coder d))
  (def v (nk/encode c #{"abernathy" "homeless"}))
  (nk/decode c v)
  ;(defn- d [] (nk/separate-data 0.2 (take 10 (load-dataset))))
  (nk/separate-data 0.5 [:a :b :c :d] [false true false false]))
