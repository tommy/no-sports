(ns no-sports.export
  "Contains functions for exporting a timeline to csv so it may be graded and
  used to train a classifier."
  (:require
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [no-sports.twitter :as twitter]
    [no-sports.util :as u]))

(defn export-csv
  "Export tweets to a csv for manual grading and use in model training."
  [n & {:keys [out-file screen-name]
        :or {out-file "resources/lubbockonline.csv"
             screen-name "lubbockonline"}}]
  (let [tweets (twitter/user-timeline n {:screen_name screen-name})
        header-line ["id" "grade" "text" "url"]
        line-fn (juxt :id
                      (constantly "")
                      (comp u/remove-newlines twitter/text)
                      (comp :expanded_url first :urls :entities))]
    (with-open [out-file (io/writer out-file)]
      (csv/write-csv out-file
                     (concat [header-line] (map line-fn tweets))
                     :quote? (constantly true)))))

(defn export-edn
  "Export tweet objects from a user timeline to a file."
  [n & {:keys [out-file screen-name]
        :or {out-file "resources/lubbockonline.edn"
             screen-name "lubbockonline"}}]
  (let [tweets (twitter/user-timeline n {:screen_name screen-name})]
    (spit out-file (with-out-str (pprint tweets)))))
