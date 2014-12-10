(ns no-sports.classification-test
  (:require [clojure.test :refer :all]
            [no-sports.classification :refer :all]
            [no-sports.data :refer :all]))

(def training-data (load-data "training.csv"))
(def grading-data (load-data "grading.csv"))

(deftest model-quality
  (let [{:keys [net coder promise pred eval-fn]}
        (trained-net training-data)]
    (deref promise 5000 nil)
    (is (<= 88/101 (eval-fn grading-data)))))
