(ns no-sports.twitter-test
  (:require [no-sports.twitter :refer [timeline]]
            [clojure.test :refer :all]))

(defn- verify-timeline
  "Verify exactly n distinct tweets are returned
  when called with given opts."
  [n & [{:as opts}]]
  (let [tweets (timeline n opts)]
    (is (= n
           (count tweets)
           (count (into #{} (map :id tweets)))))))

(deftest timeline-test
  (testing "timeline returns correct number of unique tweets"
    (testing "when they fit on one page"
      (verify-timeline 19))

    (testing "when they don't fit on one page"
      (verify-timeline 20 {:count 8}))))
