(ns no-sports.classification
  (:require [no-sports.data :refer [load-data
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

(defn- sport?
  [net tokens]
  {:pre [(set? tokens)]}
  (->> tokens
       (nk/encode token-coder)
       (nk/think net)
       (nk/decode bool-coder)))

(defn trained-net
  "Returns a promise that delivers a neural net when it finishes
  training on the given dataset."
  [dataset]
  (let [net (nk/neural-network :inputs (count (all-tokens dataset))
                               :outputs 2
                               :hidden-op Ops/LOGISTIC
                               :output-op Ops/LOGISTIC
                               :hidden-sizes [20])
        task (nk/mapping-task dataset
                              :input-coder (token-set-coder dataset)
                              :output-coder bool-coder)
        trainer (nk/supervised-trainer net task :batch-size 100)
        executable (t/task {:repeat 200} (do (trainer net) net))]
    (t/run-task executable)
    (:promise executable)))

(defn evaluate
  [net dataset]
  {:pre [(map? dataset)]}
  (let [net (.clone net)]
    (/
     (count
       (for [[tokens grade] dataset
             :when (= grade (sport? net tokens))] grade))
     (count dataset))))

(defn graph
  [net training grading]
  (let [fns [#(evaluate net training)]
        fns (if grading
              (conj fns #(evaluate net grading))
              fns)]
    (nkv/show
      (nkv/time-chart fns :y-max 1)
      :title "number correct classifications")))

;;;;;;;;;;;
;; for repl

(def dataset (load-data))
(def token-coder (token-set-coder dataset))
(def bool-coder (nk/class-coder :values #{"y" "n"}))
(def net
  (nk/neural-network :inputs (count (all-tokens dataset)) 
                     :outputs 2
                     :hidden-op Ops/LOGISTIC
                     :output-op Ops/LOGISTIC
                     :hidden-sizes [20]))
(def task
  (nk/mapping-task dataset
                   :input-coder token-coder
                   :output-coder bool-coder))
(def trainer (nk/supervised-trainer net task :batch-size 100))

(comment
  (nkv/show (nkv/network-graph net :line-width 2)
            :title "Net")

  (graph net dataset (load-data "grading.csv"))
  (evaluate @(trained-net dataset) dataset)

  (t/run {:sleep 1 :repeat 1000} (trainer net))
  (evaluate net dataset)

  (def a (atom 0))
  (deref a)
  (t/run {:while (constantly false)} (swap! a inc))
  (t/run {:repeat 100 :while (constantly false)} (swap! a inc))

  (t/stop-all)
  (do
    (t/run {:repeat 1000} (trainer net))
    (Thread/sleep 1000)
    (evaluate net dataset))

  (map (comp (partial sport? net) token-set) dataset)
  (def d (load-dataset))
  (def c (token-set-coder d))
  (def v (nk/encode c #{"abernathy" "homeless"}))
  (nk/decode c v)
  ;(defn- d [] (nk/separate-data 0.2 (take 10 (load-dataset))))
  (nk/separate-data 0.5 [:a :b :c :d] [false true false false]))
