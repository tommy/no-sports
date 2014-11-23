(ns no-sports.twitter
  "Namespace containing twitter operations."
  (:require [clojure.tools.reader.edn :as edn]
            [oauth.client :as oauth]
            [twitter.oauth :refer [make-oauth-creds]]
            [twitter.api.restful :refer [users-show
                                         statuses-user-timeline]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OAuth and Cred management

(defn- creds-from-config
  [config]
  (apply make-oauth-creds
    ((juxt :app-consumer-key
           :app-consumer-secret
           :nosportsaj-token
           :nosportsaj-secret)
     config)))

(def ^:private creds
  (let [secrets (edn/read-string (slurp "secrets.edn"))]
    (creds-from-config secrets)))

(comment
  ;; Use these calls to generate new user access tokens
  (def consumer (oauth/make-consumer
                  (:app-consumer-key secrets)
                  (:app-consumer-secret secrets)
                  "https://api.twitter.com/oauth/request_token"
                  "https://api.twitter.com/oauth/access_token"
                  "https://api.twitter.com/oauth/authorize"
                  :hmac-sha1))

  (do (def request-token (oauth/request-token consumer)) request-token)
  (oauth/user-approval-uri consumer (:oauth_token request-token))
  (oauth/access-token consumer request-token "7121424"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reading @lubbockonline's timeline

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


;;;;;;;;
;; dev

(comment
  (twitter.api.restful/friends-list :oauth-creds creds)
  (map :text (timeline 10 {:count 10}))
  (clojure.pprint/pprint (timeline 10))
  (count (timeline 10 {:count 10})))
