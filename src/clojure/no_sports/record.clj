(ns no-sports.record
  (:require [no-sports.data :refer [load-edn]]
            [no-sports.twitter :refer [listen!]]
            [no-sports.util :refer [pipe]]
            [clojure.core.async :refer [<! <!! go-loop]]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn])
  (:import java.io.PushbackReader))

(defn- swallow-eof
  [seq]
  (try
    (cons (first seq) (lazy-seq (swallow-eof (rest seq))))
    (catch Exception e
      (when-not (and (= :reader-exception (:type (ex-data e)))
                     (= (.getMessage e) "EOF"))
        (throw e)))))

(defn- edn-seq*
  [reader]
  (lazy-seq (cons (edn/read reader) (edn-seq* reader))))

(defn edn-seq
  [n]
  (-> n io/resource io/reader PushbackReader. edn-seq* swallow-eof))

(defn record
  [out]
  (let [tweets (listen! :timeout nil)
        writer (io/writer out)]
    (go-loop []
      (if-let [v (<!! tweets)]
        (do (.write writer (prn-str v))
            (.flush writer)
            (recur))
        (do (.close writer)
            (println "Closing writer for " out))))))

(comment
  (record "resources/recorded.edn")
  (def s (edn-seq "recorded.edn"))
  (def ts (clojure.tools.reader.edn/read-string (slurp "resources/lubbockonline.edn")))
  (def as (sort-by :id (into (vec s) ts)))
  (spit "resources/simulated-stream.edn"
        (with-out-str (clojure.pprint/pprint as)))
  
  (spit "resources/simulated-stream-expected.edn"
        (with-out-str (clojure.pprint/pprint
                        (map :text as)))))
