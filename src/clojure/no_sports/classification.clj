(ns no-sports.classification
  (:require [no-sports.export :refer [load-dataset
                                      el]]
            ;[clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [task.core :as t]
            [nuroko.lab.core :as nk]
            [nuroko.gui.visual :as nkv])
  (:import [no_sports.coders.SetCoder])
  (:import [mikera.vectorz Op Ops]))

(defn- token-set
  "Given a row from the dataset, returns a set of all tokens of the tweet text."
  [row]
  (as-> row ?
    (if (coll? ?) (el :text ?) ?)
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

(def dataset (load-dataset))
(def token-coder (token-set-coder dataset))
(def bool-coder (nk/class-coder :values #{"y" "n"}))
(defn- sport?
  [net text]
  (->> text
       (nk/encode token-coder)
       (nk/think net)
       (nk/decode bool-coder)))

(def net
  (nk/neural-network :inputs (count (all-tokens dataset)) 
                     :outputs 2
                     :hidden-op Ops/LOGISTIC
                     :output-op Ops/LOGISTIC
                     :hidden-sizes [20]))

(defn evaluate-scores
  [net dataset]
  (let [net (.clone net)]
    (count
      (for [[_ grade tokens _ _] dataset
            :when (= grade (sport? net (token-set tokens)))] grade))))

(def dataset-map
  (reduce
    (fn [acc row] (assoc acc (token-set row) (el :grade row)))
    {}
    dataset))

(def task
  (nk/mapping-task dataset-map
                   :input-coder token-coder
                   :output-coder bool-coder))

(def trainer (nk/supervised-trainer net task :batch-size 100))

(comment
  (nkv/show (nkv/network-graph net :line-width 2)
            :title "Net")

  (nkv/show (nkv/time-chart
              [#(evaluate-scores net dataset)]
              :y-max 100)
            :title "number correct classifications")

  (t/run {:sleep 1 :repeat 1000} (trainer net))

  (sport? net (token-set (first dataset)))
  (map (comp (partial sport? net) token-set) dataset)
  (evaluate-scores net dataset)
  (def d (load-dataset))
  (def c (token-set-coder d))
  (def v (nk/encode c #{"abernathy" "homeless"}))
  (nk/decode c v)
  ;(defn- d [] (nk/separate-data 0.2 (take 10 (load-dataset))))
  (nk/separate-data 0.5 [:a :b :c :d] [false true false false]))
