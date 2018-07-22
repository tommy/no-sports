(ns no-sports.new-twitter
  (:require
    [clojure.tools.logging :as log]
    [clojure.edn :as edn]
    [clojure.core.async :as async :refer [>!! <!! >! <! go]]
    [clj-http.client :as http]
    [oauth.client :as oauth]))


;; OAuth

(def ^:private secrets (edn/read-string (slurp "secrets.edn")))

(def consumer
  (oauth/make-consumer
    (:app-consumer-key secrets)
    (:app-consumer-secret secrets)
    "https://api.twitter.com/oauth/request_token"
    "https://api.twitter.com/oauth/access_token"
    "https://api.twitter.com/oauth/authorize"
    :hmac-sha1))

(def creds
  (partial oauth/credentials
           consumer
           (get-in secrets [:nosportsaj :token])
           (get-in secrets [:nosportsaj :secret])))

(defn- oauth1-request
  [req]
  (let [{:keys [method url query-params]} req
        auth-params (creds method url query-params)]
    (update req :query-params merge auth-params)))

(defn request
  [method url req]
  (try
    (log/debugf "Calling %s %s" method url)
    (http/request (-> req
                      (merge {:method method :url url})
                      (oauth1-request)))
    (catch Exception e
      (log/error e "Exception calling Twitter")
      nil)))


;; Read actions

(defn user-timeline*
  [params]
  (let [url "https://api.twitter.com/1.1/statuses/user_timeline.json"
        params (merge {:trim_user       true
                       :include_rts     false
                       :exclude_replies true
                       :count           200
                       :tweet_mode      "extended"}
                      params)]
    (-> (request :get url {:as :json :query-params params})
        (:body))))

(defn user-timeline
  "Returns the latest n tweets from the user_timeline given by the parameters."
  [n params]
  (loop [tweets []
         max-id (:max-id params)]
    (let [page (user-timeline* (if max-id
                                 (assoc params :max_id max-id)
                                 params))]
      (if (or (<= n (count tweets))
              (empty? page))
        (take n (into tweets page))
        (recur (into tweets page)
               (:id (last page)))))))


;; Streaming

(defn listen!
  ([screen-name]
   (listen! screen-name {}))
  ([screen-name
    {:keys [interval buffer]
     :or {interval 30000
          buffer 10}}]
   (let [chan (async/chan buffer)
         cancel? (atom false)
         cancel (fn [] (reset! cancel? true))
         latest-id (-> (user-timeline* {:screen_name screen-name
                                        :count 1})
                       (first)
                       (:id))]
     (async/go-loop [params {:screen_name screen-name
                             :since_id latest-id}]
       (when-not @cancel?
         (let [page (user-timeline* params)]
           (if (empty? page)
             (do (<! (async/timeout interval))
                 (recur params))
             (do (<! (async/onto-chan chan page false))
                 (-> params
                     (assoc :since_id (apply max (map :id page)))
                     (recur)))))))
     {:tweets chan
      :cancel cancel})))


;; Update actions

(defn retweet
  [id]
  (let [url (format "https://api.twitter.com/1.1/statuses/retweet/%s.json" id)]
    (-> (request :post url {:as :json})
        (:body))))

(defn unretweet
  [id]
  (let [url (format "https://api.twitter.com/1.1/statuses/unretweet/%s.json" id)]
    (-> (request :post url {:as :json})
        (:body))))


;; Tweet objects

(def text :full_text)

(def reply? (comp boolean :in_reply_to_user_id_str))
