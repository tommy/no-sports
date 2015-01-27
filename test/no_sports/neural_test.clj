(ns no-sports.neural-test
  (:require [clojure.test :refer :all]
            [no-sports.neural :refer :all]
            [no-sports.data :refer :all]))

(def training-data (load-data "training.csv"))
(def grading-data (load-data "grading.csv"))

(deftest model-quality
  (let [{:keys [net coder promise pred eval-fn]}
        (trained-net training-data)]
    (deref promise 5000 nil)

    (testing "model quality"
      (is (<= 88/101 (eval-fn grading-data))))

    (testing "sanity check of returned values"
      (is (not (nil? coder)))
      (is (pred "Four-star S and Texas Tech target Kahlil Haughton to make commitment at 3 p.m.:
                http://t.co/SOkssarQkJ"))
      (is (not (pred "The Following Are The Obituaries For Sunday, December 21st, 2014
                     In The Lubbock Avalanche Journal &amp; Lubbock Online:
                     http://t.co/1OOCwHfPAX"))))))
