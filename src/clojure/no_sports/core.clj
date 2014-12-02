(ns no-sports.core
  (:require [clojure.core.async :as async :refer [<! <!! go go-loop chan]]
            [no-sports.util :refer [pipe]]
            [no-sports.twitter :refer [listen!
                                       retweet retweet?
                                       tweet? tweeter=]]))

(def rt-xform
  (comp (filter tweet?)
        (remove retweet?)
        (filter (tweeter= "lubbockonline"))
        (map retweet)))

(defn -main
  "Listens to the @NoSportsAJ user stream and retweets any tweets that match
  the above transducer.
  
  Listens forever, and if it gets disconnected, reconnects.
  Blocks the current thread."
  []
  (let [connect #(pipe (listen! :timeout nil) 20 rt-xform)]
    (println "Connecting to stream...")
    (loop [stream (connect)]
      (if-let [v (<!! stream)]
        (do (println (format "Retweeting: %s" (get-in v [:body :text])))
            (recur stream))
        (do (println "Lost connection!!!")
            (recur (connect)))))))
