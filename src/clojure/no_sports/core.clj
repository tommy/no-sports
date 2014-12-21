(ns no-sports.core
  (:require [clojure.core.async :as async :refer [<! <!! go go-loop chan]]
            [clojure.pprint :refer [pprint]]
            [no-sports.util :refer [pipe tap]]
            [no-sports.data :refer [load-data load-edn]]
            [no-sports.classification :refer [trained-net]]
            [no-sports.twitter :refer [listen!
                                       retweet retweet?
                                       tweet? tweeter=]])
  (:gen-class))

(defn verify
  [pred]
  (let [failures (remove #(= (second %) (pred (first %)))
                         (load-edn "verification.edn"))]
    (when (seq failures)
      (println "Failed verification for following tweets:")
      (pprint failures))))

(defonce sport?
  (let [{:keys [promise pred eval-fn]}
        (trained-net (load-data "training.csv"))]
    (println "Training neural net...")
    (deref promise)
    (println "Done.")
    pred))

(def rt-xform
  (comp (filter tweet?)
        (tap "Got a tweet: %s" :text)
        (remove retweet?)
        (filter (tweeter= "lubbockonline"))
        (tap "Was from @lubbockonline.")
        (filter (comp sport? :text))
        (tap "Is not about sports.")
        (map retweet)))

(defn -main
  "Listens to the @NoSportsAJ user stream and retweets any tweets that match
  the above transducer.

  Listens forever, and if it gets disconnected, reconnects.
  Blocks the current thread."
  []
  (verify sport?)
  (let [connect #(pipe (listen! :timeout nil) 20 rt-xform)]
    (println "Connecting to stream...")
    (loop [stream (connect)]
      (if-let [v (<!! stream)]
        (do (println (format "Retweeting: %s" (get-in v [:body :text])))
            (recur stream))
        (do (println "Lost connection!!!")
            (recur (connect)))))))
