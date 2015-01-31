(ns no-sports.export
  "Contains functions for exporting a timeline to csv
  so it may be graded and used to train a classifier."
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]
            [clojure.pprint :refer [pprint]]
            [no-sports.twitter :refer :all]
            [no-sports.util :refer [remove-newlines]]))

(defn export-csv
  "Export tweets to a csv for manual grading and use in model training."
  [& {:keys [out-file page-size max-id]
      :or {out-file "resources/lubbockonline.csv"
           page-size 101}}]
  (let [tweets (timeline 200 {:count page-size})
        line-fn (juxt :id
                      (constantly "")
                      (comp remove-newlines :text)
                      (comp :expanded_url first :urls :entities))]
    (with-open [out-file (io/writer out-file)]
      (csv/write-csv out-file
                     (map line-fn tweets)
                     :quote? (constantly true)))))

(defn export-edn
  "Export tweet objects from a user timeline to a file."
  [n & {:keys [out-file page-size max-id]
        :or {out-file "resources/lubbockonline.edn"
             page-size (min n 101)}}]
  (let [tweets (timeline n {:count page-size})]
    (spit out-file (with-out-str (pprint tweets)))))
