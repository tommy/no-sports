(ns no-sports.core
  (:require
    [clojure.core.async :as async :refer [<!!]]
    [clojure.tools.logging :as log]
    [clojure.pprint :refer [pprint]]
    [no-sports.util :refer [pipe tap report]]
    [no-sports.data :refer [load-data load-edn]]
    [no-sports.bayes :refer [classify-pred]]
    [no-sports.backoff :refer [backoff split]]
    [no-sports.twitter :refer [listen! text retweet]])
  (:gen-class))

(defn verify
  [pred]
  (let [failures (remove #(= (second %) (pred (first %)))
                         (load-edn "verification.edn"))]
    (when (seq failures)
      (log/error "Failed verification for following tweets:")
      (pprint failures))))

(def sport?
  (-> (load-data "training.csv")
      (classify-pred :y)))

(def rt-xform
  (comp (report "https://nosnch.in/596c61db7e")
        (tap "Got a tweet: %s" [text])
        (remove (comp nil? text))
        (remove (comp sport? text))
        (tap "Is not about sports.")))

(defn -main
  "Polls the @NoSportsAJ user timeline and retweets any tweets that match
  the above transducer."
  []
  (verify sport?)
  (let [{:keys [tweets cancel]} (listen! "lubbockonline")
        stream (pipe tweets 20 rt-xform)]
    (log/info "Polling Twitter.")
    (loop []
      (if-let [v (<!! stream)]
        (do (log/infof "Retweeting: %s" (text v))
            (retweet (:id v))
            (recur))
        (log/error "Tweet channel was closed! Exiting.")))))
