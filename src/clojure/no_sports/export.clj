(ns no-sports.export
  "Contains functions for exporting a timeline to csv
  so it may be graded and used to train a classifier."
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]
            [no-sports.twitter :refer :all]
            [no-sports.util :refer [tokenize
                                    remove-newlines]]))
(defn- grade-fn
  [manual]
  (if manual
    (fn [tweet]
      (println "\n" (:text tweet) "[y/n]")
      (read-line))
    (constantly "")))

(defn export
  [& {:keys [manual out-file n max-id]
      :or {manual false
           out-file "lubbockonline.csv"
           max-id 526093035797364736
           n 101}}]
  (let [tweets (timeline 200 {:max-id max-id :count n})
        line-fn (juxt :id
                      (grade-fn manual)
                      (comp (partial join " ") tokenize :text)
                      (comp :expanded_url first :urls :entities)
                      (comp remove-newlines :text))]
    (with-open [out-file (io/writer out-file)]
      (csv/write-csv out-file
                     (map line-fn tweets)
                     :quote? (constantly true)))))
