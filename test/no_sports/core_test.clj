(ns no-sports.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.logging :refer [*logger-factory*]]
            [clojure.tools.logging.impl :refer [disabled-logger-factory]]
            [clojure.data :refer [diff]]
            [clojure.core.async :refer [chan <!! >!! close! alt!! go >! <!]]
            [no-sports.test.util :as tu]
            [no-sports.core :refer :all]
            [no-sports.util :refer [pipe]]))

(use-fixtures :once tu/mocks-fixture)

(def ^:private sample-stream
  (-> "simulated-stream.edn" io/resource slurp edn/read-string))

(def ^:private expected-rt
  (-> "simulated-stream-expected.edn" io/resource slurp edn/read-string))

(defmacro dont-print
  [& body]
  `(binding [*out* (io/writer "/dev/null")
             *logger-factory* disabled-logger-factory]
     ~@body))

(deftest test-xform
  (let [actual-rt (dont-print (doall (sequence rt-xform sample-stream)))
        [false-neg false-pos _] (diff expected-rt (set (map :text actual-rt)))]
    (when (seq false-neg)
      (is false (format "These SHOULD have been tweeted:%n%s"
                        (with-out-str (pprint false-neg)))))
    (when (seq false-pos)
      (is false (format "These SHOULD NOT have been tweeted:%n%s"
                        (with-out-str (pprint false-pos)))))))

(deftest test-reconnect
  (testing "pipe closes when tweet channel closes"
    (dont-print
      (let [tweet (first (sequence rt-xform sample-stream))
            in (chan 5)
            out (pipe in 20 rt-xform)]
        (>!! in tweet)
        (is (not= :nothing (alt!! out ([v] v) :default :nothing)))
        (close! in)
        (is (nil? (<!! out)))))))
