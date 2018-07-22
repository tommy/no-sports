(ns no-sports.util-test
  (:require
    [clojure.test :refer :all]
    [no-sports.util :as u]))

(deftest remove-newlines-test
  (is (= "1 2" (u/remove-newlines "1\n2"))))

(deftest remove-urls-test
  (is (= "hey  there" (u/remove-urls "hey http://google.com there")))
  (is (= "hey  there" (u/remove-urls "hey https://google.com there"))))
