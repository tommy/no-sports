(ns no-sports.backoff-test
  (:require
    [clojure.test :refer :all]
    [no-sports.backoff :refer :all]))

(deftest backoff-test
  (testing "generate several backoff values"
    (let [init (backoff 10 9999 10000 :timestamp 0)
          [wait-1 b-1] (split init 10)
          [wait-2 b-2] (split b-1 1000)
          [wait-3 b-3] (split b-2 10000)
          [wait-4 b-4] (split b-3 10001)
          [wait-5 b-5] (split b-4 20002)]

      (is (= [10 20 40 80 10] [wait-1 wait-2 wait-3 wait-4 wait-5]))
      (are [k vs] (= vs (map k [init b-1 b-2 b-3 b-4 b-5]))
        :attempt [0 1 2 3 4 1]
        :timestamp [0 10 1000 10000 10001 20002])))

  (testing "wait doesn't exceed max-wait"
    (let [init (backoff 20 300 10000 :timestamp 0)
          f (comp #(split % 0) second)
          vs (iterate f [0 init])
          waits (map first vs)]
      (is (= [0 20 40 80 160 300 300] (take 7 waits))))))
