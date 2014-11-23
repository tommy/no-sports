(ns no-sports.twitter
  "Namespace containing twitter operations."
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.data.json :as json]
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

#_(defn listen!
  [callback]
  (user-stream :oauth-creds creds))

(defn listen!
  [an-atom]
  (let [push-part #(swap! an-atom update-in [:part] conj %)
        push-failure #(swap! an-atom update-in [:failure] conj %)
        callback (AsyncStreamingCallback. (comp push-part second vector)
                                          (comp push-failure handlers/response-return-everything)
                                          #_(comp println handlers/response-return-everything)
                                          handlers/exception-print)]
    (twitter.api.streaming/statuses-filter
      :params {:track "Beyonce"}
      :oauth-creds creds
      :callbacks callback)))


;;;;;;;;
;; dev

(comment
  (twitter.api.restful/friends-list :oauth-creds creds)
  (map :text (timeline 10 {:count 10}))
  (clojure.pprint/pprint (timeline 10))
  (count (timeline 10 {:count 10})))
