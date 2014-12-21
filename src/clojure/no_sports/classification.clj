(ns no-sports.classification
  "This namespace trains a classification model."
  (:require [no-sports.data :refer [load-data all-tokens]]
            [no-sports.util :refer [tokenize mapk]]
            [task.core :as t]
            [nuroko.lab.core :as nk]
            [nuroko.gui.visual :as nkv])
  (:import [no_sports.coders.SetCoder])
  (:import nuroko.module.NeuralNet)
  (:import [mikera.vectorz Op Ops]))

(defn- token-set-coder
  "Creates a coder that encodes/decodes sets of tokens. Only tokens that appear
  in the original dataset (passed to this function) can be encoded.
  Unrecognized tokens are ignored."
  [dataset]
  (let [all (all-tokens dataset)]
    (no_sports.coders.SetCoder.
      ^java.util.Collection (vec all))))

(def ^:private bool-coder (nk/class-coder :values #{"y" "n"}))

(defn- run
  [net coder tokens]
  {:pre [(string? tokens)]
   :post [(#{"y" "n"} %)]}
  (->> tokens
       tokenize
       (nk/encode coder)
       (nk/think net)
       (nk/decode bool-coder)))

(def ^:private pred?
  (comp (partial = "y") run))

(defn- evaluate
  "Evaluate the performance of a trained net against a particular
  dataset."
  [net coder dataset]
  {:pre [(map? dataset)]}
  (let [net (.clone ^NeuralNet net)
        correct (for [[tokens grade] dataset
                      :when (= grade (run net coder tokens))] grade)]
    (/ (count correct)
       (count dataset))))

(defn trained-net
  "Trains a neural network based on the given dataset.
  Returns:

  {
        :net -- the neural network
      :coder -- the token-set-coder for this dataset
    :eval-fn -- a function that takes a grading dataset
                and scores the trained network on it
       :pred -- a predicate function that returns true if
                the argument is about sports
    :promise -- a promise that is delivered when training completes
  }"
  [dataset]
  {:pre [(map? dataset)
         (every? string? (keys dataset))
         (every? #{"y" "n"} (vals dataset))]}
  (let [net (nk/neural-network :inputs (count (all-tokens dataset))
                               :outputs 2
                               :hidden-op Ops/LOGISTIC
                               :output-op Ops/LOGISTIC
                               :hidden-sizes [20])
        coder (token-set-coder dataset)
        task (nk/mapping-task (mapk tokenize dataset)
                              :input-coder coder
                              :output-coder bool-coder)
        trainer (nk/supervised-trainer net task :batch-size 100)
        executable (t/task {:repeat 200} (do (trainer net) net))]
    (t/run-task executable)
    {:net net
     :coder coder
     :eval-fn (partial evaluate net coder)
     :pred (partial pred? net coder)
     :promise (:promise executable)}))


;;;;;;
;; DEV

(defn- graph
  "Display a graph of the performance of a neural net as it is being trained."
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
  (def d (remove #(= % (tokenize (clojure.string/join " " %)))
                 (keys grading)))
  (map (juxt identity #(tokenize (clojure.string/join " " %))) d)

  (def training (load-data "training.csv"))
  (def grading (load-data "grading.csv"))
  (def grading-n (->> grading
                      (filter #(= "n" (val %)))
                      (into {})))
  (def grading-y (->> grading
                      (filter #(= "y" (val %)))
                      (into {})))
  (let [{:keys [promise eval-fn]} (trained-net training)]
    @promise
    (double (eval-fn grading-y)))

  (let [{:keys [net coder]} (trained-net (load-data "training.csv"))]
    (graph net coder training grading (load-data "grading.csv")))

  (def net (:net (trained-net training)))
  (nkv/show (nkv/network-graph net :line-width 2)
            :title "Net")

  (t/stop-all)

  (nk/separate-data 0.5 [:a :b :c :d] [false true false false]))
