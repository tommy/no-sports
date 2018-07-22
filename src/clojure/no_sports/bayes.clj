(ns no-sports.bayes
  (:require
    [no-sports.data :refer [load-data]]
    [judgr.core :as judgr]
    [judgr.settings :as judgr.s])
  (:import
    (judgr.classifier.default_classifier
      DefaultClassifier)))

(defn classifier
  [dataset]
  (let [s (judgr.s/update-settings judgr.s/settings
                                   [:classes] [:y :n]
                                   [:classifier :default :thresholds] {:y 1 :n 5})
        classifier (judgr/classifier-from s)
        items (map (fn [[k v]] {:item k :class (keyword v)}) dataset)]
    (.train-all! classifier items)
    classifier))

(defn classify-pred
  [dataset-or-classifier expected-val]
  (let [c (condp = (type dataset-or-classifier)
            DefaultClassifier dataset-or-classifier
            (classifier dataset-or-classifier))]
    (fn [v] (= expected-val (.classify c v)))))

(comment
  (def d (load-data "training.csv"))
  (def g (load-data "grading.csv"))
  (do (def c (classifier d))
      (judgr.cross-validation/k-fold-crossval 3 c))
  {:n {:unknown 18, :n 201, :y 19}, :y {:unknown 7, :n 6, :y 148}}
  {:n {:unknown 8, :n 217, :y 14}, :y {:unknown 3, :n 12, :y 145}}
  {:n {:unknown 5, :n 225, :y 9}, :y {:unknown 15, :n 10, :y 135}})
