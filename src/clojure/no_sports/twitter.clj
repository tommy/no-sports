(ns no-sports.twitter
  "Namespace containing twitter operations."
  (:require [clojure.pprint :refer [pprint]])
  (:require [no-sports.util :refer [parse-json]]
            [clojure.tools.reader.edn :as edn]
            [clojure.core.async :refer [go-loop <! >!! chan close!]]
            [cheshire.core :as json]
            [oauth.client :as oauth]
            [twitter.oauth :refer [make-oauth-creds]]
            [twitter.api.restful :refer [users-show
                                         statuses-user-timeline]]
            [twitter.api.streaming :refer [user-stream]]
            [twitter.callbacks.handlers :as handlers])
  (:import twitter.callbacks.protocols.AsyncStreamingCallback))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OAuth and Cred management

(defn- creds-from-config
  [user config]
  (apply make-oauth-creds
    ((juxt :app-consumer-key
           :app-consumer-secret
           #(get-in % [user :token])
           #(get-in % [user :secret]))
     config)))

(def ^:private secrets (edn/read-string (slurp "secrets.edn")))
(def ^:private creds (creds-from-config :nosportsaj secrets))

(comment
  ;; Use these calls to generate new user access tokens
  (let [secrets (edn/read-string (slurp "secrets.edn"))]
    (creds-from-config :nosportsaj secrets)
    (def consumer (oauth/make-consumer
                    (:app-consumer-key secrets)
                    (:app-consumer-secret secrets)
                    "https://api.twitter.com/oauth/request_token"
                    "https://api.twitter.com/oauth/access_token"
                    "https://api.twitter.com/oauth/authorize"
                    :hmac-sha1)))

  (do (def request-token (oauth/request-token consumer)) request-token)
  (oauth/user-approval-uri consumer (:oauth_token request-token))
  (oauth/access-token consumer request-token "4055778"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reading @lubbockonline's timeline (non-streaming)

(defn- timeline*
  "Fetches a single page of tweets from a user's timeline."
  [opts]
  (statuses-user-timeline
    :oauth-creds creds
    :params (merge {:screen-name "lubbockonline"
                    :count 100
                    :trim-user true}
                   opts)))

(defn timeline
  "Fetches at least n items from a user's timeline.

  Automatically fetchs additional pages if necessary."
  [n & [{:as opts}]]
  (loop [tweets []
         opts (or opts {})]
    (let [page (timeline* opts)]
      (if (and (seq (:body page))
               (< (count tweets) n))
        (recur (into tweets (:body page))
               {:max-id (max (map :id (:body page)))})
        tweets))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; streaming feed for @lubbockonline

(defn listen!
  "Open a streaming connection and return the parsed tweets on a channel."
  [& {:keys [timeout]
      :or {timeout 10000}}]
  (let [ch (chan 10)
        write (comp #(>!! ch %) str second vector)
        callback (AsyncStreamingCallback. write
                                          (comp println handlers/response-return-everything)
                                          handlers/exception-print)
        stream (twitter.api.streaming/statuses-filter
                 :params {:track "Beyonce" :delimited true}
                 :oauth-creds creds
                 :callbacks callback)
        cancel #(do ((:cancel (meta stream)))
                    (close! ch))]
    (future (do (Thread/sleep timeout)
                (cancel)))
    (parse-json ch)))


;;;;;;;;
;; dev

(defn spit-pp
  "Pretty print the given data structure to a file."
  [f data]
  (spit f (with-out-str (pprint data))))

(defn print-tweets
  "Read tweets from a channel and print the text."
  [ch]
  (go-loop []
    (when-let [v (<! ch)]
      (println (:text v))
      (recur))))

(comment

  (let [output (java.io.PipedOutputStream.)
        input (java.io.BufferedReader. (java.io.InputStreamReader. (java.io.PipedInputStream. output)))]
    (future (.write output (.getBytes "hihihi\n")))
    (future (println (.readLine input))))

  (do (def res (atom {}))
      (let [ctrl (listen! res)]
        (Thread/sleep 10000)
        ((:cancel (meta ctrl)))))
  (->> res deref :part (map #(-> % :body deref str json/read-json :id)) set)
  (let [s (->> res deref :part (map str) reverse (clojure.string/join ""))]
       (pprint (json/parsed-seq (java.io.StringReader. s))))

  (let [x (->> res deref :part (map #(-> % :body deref str)) reverse)]
    (count x)
    (count (set x)))

  (spit-pp "/tmp/part.edn"
    (->> res deref :part (map str) #_(map #(-> % :body deref str)) reverse))


  (twitter.api.restful/friends-list :oauth-creds creds)
  (map :text (timeline 10 {:count 10}))
  (clojure.pprint/pprint (timeline 10))
  (count (timeline 10 {:count 10})))
