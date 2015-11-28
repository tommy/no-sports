(ns no-sports.core
  (:require [clojure.core.async :as async :refer [<! <!! go go-loop chan timeout]]
            [clojure.tools.logging :refer [info infof warnf error]]
            [clojure.pprint :refer [pprint]]
            [no-sports.util :refer [pipe tap]]
            [no-sports.data :refer [load-data load-edn]]
            [no-sports.bayes :refer [classify-pred]]
            [no-sports.backoff :refer [backoff split]]
            [no-sports.twitter :refer [listen!
                                       retweet retweet?
                                       tweet? tweeter=]])
  (:gen-class))

(defn verify
  [pred]
  (let [failures (remove #(= (second %) (pred (first %)))
                         (load-edn "verification.edn"))]
    (when (seq failures)
      (error "Failed verification for following tweets:")
      (pprint failures))))

(def sport?
  (-> (load-data "training.csv")
      (classify-pred :y)))

(def rt-xform
  (comp (filter tweet?)
        (tap "Got a tweet: %s" :text)
        (remove retweet?)
        (filter (tweeter= "lubbockonline"))
        (tap "Was from @lubbockonline.")
        (remove (comp sport? :text))
        (tap "Is not about sports.")))

(def reconnect-interval (* 12 60 60 1000)) ; always reconnect every 12 hours
(defn -main
  "Listens to the @NoSportsAJ user stream and retweets any tweets that match
  the above transducer.

  Listens forever, and if it gets disconnected, reconnects.
  Blocks the current thread."
  []
  (verify sport?)
  (let [connect #(pipe (listen! :timeout reconnect-interval) 20 rt-xform)]
    (info "Connecting to stream...")
    (loop [stream (connect)
           ;; for each reconnect, wait a minimum of 1 second, a maximum of 10 minutes,
           ;; and reset the timer after staying connected for 2 hours.
           delay-timer (backoff 1000 (* 10 60 1000) (* 2 60 60 1000))]
      (if-let [v (<!! stream)]
        (do (infof "Retweeting: %s" (:text v))
            (retweet v)
            (recur stream delay-timer))
        (let [[wait new-delay-timer] (split delay-timer)]
          (warnf "Lost connection!!! Reconnecting in %d ms" wait)
          (<!! (timeout wait))
          (recur (connect) new-delay-timer))))))
