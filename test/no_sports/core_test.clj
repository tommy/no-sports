(ns no-sports.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [clojure.data :refer [diff]]
            [no-sports.core :refer :all]))

(def ^:private sample-stream
  (-> "simulated-stream.edn" io/resource slurp edn/read-string))

(def ^:private expected-rt
  (-> "simulated-stream-expected.edn" io/resource slurp edn/read-string))

(defmacro dont-print
  [& body]
  `(binding [*out* (io/writer "/dev/null")]
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
