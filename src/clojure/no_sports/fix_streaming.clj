(ns no-sports.fix-streaming
  "TODO this is only needed to change API version from 2 to 1.1. Once twitter-api
  version 0.7.9 is deployed, this can be deleted."
  (:require [twitter.api.streaming :as s]
            [twitter.core :as twitter]
            [twitter.callbacks :as callbacks])
  (:import twitter.api.ApiContext))

(def ^:dynamic *user-stream-api* (ApiContext. "https" "userstream.twitter.com" "1.1"))

(defmacro def-twitter-user-streaming-method
  "defines a user streaming method using the above context"
  [name verb resource-path & rest]

  `(twitter/def-twitter-method ~name ~verb ~resource-path :api ~*user-stream-api* :callbacks (callbacks/get-default-callbacks :async :streaming) ~@rest))

(def-twitter-user-streaming-method user-stream :get "user.json")
