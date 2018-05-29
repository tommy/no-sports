(ns no-sports.util-test
  (:require [no-sports.util :as u]
            [clojure.test :refer :all]
            [clojure.core.async :refer [chan <!! >!! close!]]))

(deftest parse-json-test
  (testing "can parse channel of partial json messages"
    (let [source (chan)
          json (u/parse-json source)]
      (>!! source "{\"a\":1}")
      (is (= {:a 1} (<!! json)))

      (>!! source "{\"b\"")
      (>!! source ":2}")
      (is (= {:b 2} (<!! json)))

      (close! source)
      (is (nil? (<!! json)))))

  (testing "output channel closes if source closes"
    (let [source (chan)
          json (u/parse-json source)]
      (>!! source "{\"b\"")

      (close! source)
      (is (nil? (<!! json))))))

(deftest remove-newlines-test
  (is (= "1 2" (u/remove-newlines "1\n2"))))

(deftest remove-urls-test
  (is (= "hey  there" (u/remove-urls "hey http://google.com there")))
  (is (= "hey  there" (u/remove-urls "hey https://google.com there"))))

(deftest escaped-test
  (is (= "hey\\nthere" (u/escaped "hey\nthere"))))

(deftest whitespace-test
  (is (u/whitespace? "\r\n"))
  (is (not (u/whitespace? "f\n")))
  (is (not (u/whitespace? "\na"))))
