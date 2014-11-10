(ns no-sports.export
  (:require [twitter.oauth :refer [make-oauth-creds]]
            [twitter.api.restful :refer [users-show
                                         statuses-user-timeline]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [no-sports.core :refer [normalize-text
                                    remove-newlines]]))

(def creds
  (make-oauth-creds ""
                    ""
                    ""
                    ""))

(defn timeline
  [opts]
  (statuses-user-timeline :oauth-creds creds
                          :params (merge {:screen-name "lubbockonline"
                                          :count 100}
                                         opts)))

(defn history
  [n & [{:as opts}]]
  (loop [tweets []
         opts (or opts {})]
    (let [page (timeline opts)]
      (if (and (seq (:body page)) (< (count tweets) n))
        (recur (into tweets (:body page))
               {:max-id (max (map :id (:body page)))})
        tweets))))

(defn export
  [& {:keys [manual]}]
  (println manual)
  (let [grade-fn (if manual
                   (fn [tweet]
                     (println "\n" (:text tweet) "[y/n]")
                     (read-line))
                   (constantly ""))
        tweets (history 100 {:max-id 528780556280946688 :count 101})
        line-fn (juxt :id
                      grade-fn
                      (comp normalize-text :text)
                      (comp :url first :urls :entities)
                      (comp remove-newlines :text))]
    (with-open [out-file (io/writer "lubbockonline-grading.csv")]
      (csv/write-csv out-file
                     (map line-fn tweets)
                     :quote? (constantly true)))))

(comment
  (map :id (:body (timeline {:max-id 528780556280946688})))
  (clojure.pprint/pprint (timeline {}))
  (count (:body (timeline {:count 100}))))
