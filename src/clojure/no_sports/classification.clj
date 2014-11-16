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

(def bool-coder (nk/class-coder :values #{"y" "n"}))

(defn- sport?
  [net coder tokens]
  {:pre [(set? tokens)]}
  (->> tokens
       (nk/encode coder)
       (nk/think net)
       (nk/decode bool-coder)))

(defn evaluate
  "Evaluate the performance of a trained net against a particular
  dataset."
  [net coder dataset]
  {:pre [(map? dataset)]}
  (let [net (.clone net)]
    (/
     (count
       (for [[tokens grade] dataset
             :when (= grade (sport? net coder tokens))] grade))
     (count dataset))))

(defn trained-net
  "Returns a promise that delivers a neural net when it finishes
  training on the given dataset."
  [dataset]
  (let [net (nk/neural-network :inputs (count (all-tokens dataset))
                               :outputs 2
                               :hidden-op Ops/LOGISTIC
                               :output-op Ops/LOGISTIC
                               :hidden-sizes [20])
        coder (token-set-coder dataset)
        task (nk/mapping-task dataset
                              :input-coder coder
                              :output-coder bool-coder)
        trainer (nk/supervised-trainer net task :batch-size 100)
        executable (t/task {:repeat 200} (do (trainer net) net))]
    (t/run-task executable)
    {:net net
     :coder coder
     :eval-fn (partial evaluate net coder)
     :pred (partial sport? net coder)
     :promise (:promise executable)}))

(defn graph
  [net coder & gradings]
  {:pre [(seq gradings)
         (every? map? gradings)]}
  (let [fns (map (fn [d] #(evaluate net coder d)) gradings)]
    (nkv/show
      (nkv/time-chart fns :y-max 1)
      :title "% correct classifications")))

;;;;;;;;;;;
;; for repl

(comment
  (def dataset (load-data))
  (def token-coder (token-set-coder dataset))
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
  (def trainer (nk/supervised-trainer net task :batch-size 100)))

(comment
  (nkv/show (nkv/network-graph net :line-width 2)
            :title "Net")

  (let [{:keys [promise eval-fn]} (trained-net dataset)]
    @promise
    (eval-fn dataset))

  (def training (load-data "training.csv"))
  (def grading (load-data "grading.csv"))
  (let [{:keys [promise eval-fn]} (trained-net training)]
    @promise
    (eval-fn grading))

  (let [{:keys [net coder]} (trained-net training)]
    (graph net coder training grading (load-data "all.csv")))

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

  (map (comp (partial sport? net dataset) token-set) dataset)
  (def d (load-dataset))
  (def c (token-set-coder d))
  (def v (nk/encode c #{"abernathy" "homeless"}))
  (nk/decode c v)
  ;(defn- d [] (nk/separate-data 0.2 (take 10 (load-dataset))))
  (nk/separate-data 0.5 [:a :b :c :d] [false true false false]))
