(ns no-sports.bayes-test
  (:require
    [clojure.test :refer :all]
    [judgr.cross-validation :as judgr]
    [no-sports.bayes :refer :all]
    [no-sports.data :refer :all]))

(def training-data (load-data "training.csv"))

(def total
  (comp (partial reduce +) vals))

(deftest model-quality
  (let [c (classifier training-data)
        qual (judgr/k-fold-crossval 5 c)
        total-n (total (:n qual))]

    (testing "model quality"
      (is (<= (/ (get-in qual [:y :n]) total-n)
              1/100)))

    (testing "sanity check of returned values"
      (let [pred (classify-pred c :y)]
        (is (pred "Four-star S and Texas Tech target Kahlil Haughton to make commitment at 3 p.m.:
                  http://t.co/SOkssarQkJ"))
        (is (not (pred "The Following Are The Obituaries For Sunday, December 21st, 2014
                       In The Lubbock Avalanche Journal &amp; Lubbock Online:
                       http://t.co/1OOCwHfPAX")))))))
