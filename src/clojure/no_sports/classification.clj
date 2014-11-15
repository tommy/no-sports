(ns no-sports.classification
  (:require [no-sports.data :refer [load-dataset
                                    el
                                    to-map
                                    all-tokens
                                    token-set]]
            [task.core :as t]
            [nuroko.lab.core :as nk]
            [nuroko.gui.visual :as nkv])
  (:import [no_sports.coders.SetCoder])
  (:import [mikera.vectorz Op Ops]))

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



(def task
  (nk/mapping-task (to-map dataset)
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
  (evaluate-scores net dataset)

  (t/stop-all)
  (do
    (t/run {:repeat 1000} (trainer net))
    (Thread/sleep 1000)
    (evaluate-scores net dataset))

  (map (comp (partial sport? net) token-set) dataset)
  (def d (load-dataset))
  (def c (token-set-coder d))
  (def v (nk/encode c #{"abernathy" "homeless"}))
  (nk/decode c v)
  ;(defn- d [] (nk/separate-data 0.2 (take 10 (load-dataset))))
  (nk/separate-data 0.5 [:a :b :c :d] [false true false false]))
