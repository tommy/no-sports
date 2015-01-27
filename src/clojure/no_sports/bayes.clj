(ns no-sports.bayes
  (:require [no-sports.data :refer [load-data]]
            [judgr.core :refer [classifier-from]]
            [judgr.settings :refer [settings
                                    update-settings]]
            [judgr.cross-validation :refer [k-fold-crossval]])
  (:import judgr.classifier.default_classifier.DefaultClassifier))

(defn classifier
  [dataset]
  (let [s (update-settings settings
                           [:classes] [:y :n]
                           [:classifier :default :thresholds] {:y 1 :n 5})
        classifier (classifier-from s)
        items (map (fn [[k v]] {:item k :class (keyword v)}) dataset)]
    (.train-all! classifier items)
    classifier))

(defn classify-pred
  [dataset expected-val]
  (let [c (classifier dataset)]
    (fn [v] (= v (.classify c expected-val)))))

(comment
  (def d (load-data "training.csv"))
  (def g (load-data "grading.csv"))
  (do (def c (classifier d))
      (k-fold-crossval 3 c))
  {:n {:unknown 18, :n 201, :y 19}, :y {:unknown 7, :n 6, :y 148}}
  {:n {:unknown 8, :n 217, :y 14}, :y {:unknown 3, :n 12, :y 145}}
  {:n {:unknown 5, :n 225, :y 9}, :y {:unknown 15, :n 10, :y 135}})
