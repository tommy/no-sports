(ns no-sports.twitter
  "Namespace containing twitter operations."
  (:require [clojure.pprint :refer [pprint]])
  (:require [no-sports.util :refer [parse-json]]
            [clojure.edn :as edn]
            [clojure.tools.logging :refer [info error]]
            [clojure.core.async :as async :refer [go <! >!! chan close!]]
            [cheshire.core :as json]
            [oauth.client :as oauth]
            [twitter.oauth :refer [make-oauth-creds]]
            [twitter.api.restful :refer [users-show
                                         statuses-user-timeline
                                         statuses-retweet-id]]
            [twitter.callbacks.handlers :as handlers]
            [no-sports.fix-streaming :as fix])
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
                    :count 100}
                   opts)))

(defn timeline
  "Fetches n tweets from a user's timeline (or all if less than n).

  Automatically fetchs additional pages if necessary."
  [n & [{:as opts}]]
  (loop [tweets []
         opts (or opts {})]
    (let [page (:body (timeline* opts))
          tweets (into tweets page)]
      (if (and (seq page)
               (< (count tweets) n))
        (recur tweets
               (merge opts {:max-id (dec (reduce min (map :id page)))}))
        (take n tweets)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; streaming feed for @lubbockonline

(defn listen!
  "Open a streaming connection and return the parsed tweets on a channel."
  [& {:keys [timeout buffer]
      :or {timeout 10000
           buffer 10}}]
  (let [ch (chan buffer)
        write (comp #(>!! ch %) str second vector)
        ex-handler (fn [_ throwable]
                     (error throwable "Exception handler triggered.")
                     (close! ch))
        failure-handler (fn [response]
                          (error "Failure handler triggered.")
                          (error response)
                          (close! ch))
        callback (AsyncStreamingCallback. write failure-handler ex-handler)
        stream (fix/user-stream :oauth-creds creds :callbacks callback)

        cancel #(do ((:cancel (meta stream)))
                    (close! ch))

        _ (when timeout
            (go (<! (async/timeout timeout))
                (cancel)))]

    (parse-json ch)))

(comment
  (twitter.api.streaming/statuses-filter
    :params {:track "Beyonce" :delimited true}
    :oauth-creds creds
    :callbacks callback))

;;;;;;;;;;;;;
;; retweeting

(defn retweet-id
  [id]
  (try
    (statuses-retweet-id :oauth-creds creds
                         :params {:id id})
    (catch Exception e
      (error e "Exception when retweeting."))))

(def retweet (comp retweet-id :id))

(comment
  (retweet-id 488530656759009282))


;;;;;;;;;;;;;;;;;;;
;; tweet inspectors

(def retweet? :retweeted_status)
(def tweeter (comp :screen_name :user))
(defn tweet?
  [m]
  (and (contains? m :id)
       (contains? m :text)))

(defn tweeter=
  "Create a predicate that returns true when the author
  is equal to this parameter."
  [author]
  #(-> %
       tweeter
       (= author)))


;;;;;;;;
;; dev

(defn spit-pp
  "Pretty print the given data structure to a file."
  [f data]
  (spit f (with-out-str (pprint data))))

(defn print-tweets
  "Read tweets from a channel and print the text."
  [ch]
  (async/go-loop []
    (when-let [v (<! ch)]
      (println (:text v))
      (recur))))

(comment

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
