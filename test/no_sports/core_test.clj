(ns no-sports.core-test
  (:require
    [clojure.test :refer :all]
    [clojure.edn :as edn]
    [clojure.pprint :refer [pprint]]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [clojure.tools.logging.impl :as log.impl]
    [clojure.data :refer [diff]]
    [no-sports.test.util :as tu]
    [no-sports.core :as core]))

(use-fixtures :once tu/mocks-fixture)

(def ^:private sample-stream
  (-> "simulated-stream.edn" io/resource slurp edn/read-string))

(def ^:private expected-rt
  (-> "simulated-stream-expected.edn" io/resource slurp edn/read-string))

(defmacro dont-print
  [& body]
  `(binding [*out* (io/writer "/dev/null")
             log/*logger-factory* log.impl/disabled-logger-factory]
     ~@body))

(deftest test-xform
  ;; TODO: test data broken
  (let [actual-rt (dont-print (doall (sequence core/rt-xform sample-stream)))
        [false-neg false-pos _] (diff expected-rt (set (map :text actual-rt)))]
    (when (seq false-neg)
      (is false (format "These SHOULD have been tweeted:%n%s"
                        (with-out-str (pprint false-neg)))))
    (when (seq false-pos)
      (is false (format "These SHOULD NOT have been tweeted:%n%s"
                        (with-out-str (pprint false-pos)))))))
