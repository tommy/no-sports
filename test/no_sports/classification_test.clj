(ns no-sports.classification-test
  (:require [clojure.test :refer :all]
            [no-sports.classification :refer :all]
            [no-sports.data :refer :all]))

(def training-data (load-data "all.csv"))
(def grading-data (load-data "third.csv"))

(deftest model-quality
  (let [{:keys [net coder promise pred eval-fn]}
        (trained-net training-data)]
    (deref promise 2000 nil)
    (is (<= 87/98 (eval-fn grading-data)))))
