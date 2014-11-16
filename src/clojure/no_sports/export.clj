(ns no-sports.export
  (:require [twitter.oauth :refer [make-oauth-creds]]
            [twitter.api.restful :refer [users-show
                                         statuses-user-timeline]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :refer [join]]
            [no-sports.core :refer [tokenize
                                    remove-newlines]]))

(def creds
  (make-oauth-creds "htDShW8DFBzHtsSEsxWwLFHUc"
                    "GpYTdiAPWFod2JkmCwYoedefyjvuvoJaB5ERapbDKron5lP8RB"
                    "191624404-T9JVGKuqkfgcChzN7YJtbjO7kcoagsAHVqDgmXFh"
                    "tiIebSK1Sn3lbcQmDd6AMP0JUaoIvhUakL0jfB8vrFrIf"))

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
        tweets (history 200 {:max-id 526093035797364736 :count 101})
        line-fn (juxt :id
                      grade-fn
                      (comp (partial join " ") tokenize :text)
                      (comp :url first :urls :entities)
                      (comp remove-newlines :text))]
    (with-open [out-file (io/writer "lubbockonline.csv")]
      (csv/write-csv out-file
                     (map line-fn tweets)
                     :quote? (constantly true)))))


;;;;;;;;
;; dev

(comment
  (map :id (:body (timeline {:max-id 528780556280946688})))
  (clojure.pprint/pprint (timeline {}))
  (count (:body (timeline {:count 100}))))
