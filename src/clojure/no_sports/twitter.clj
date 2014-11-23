(ns no-sports.twitter
  "Namespace containing twitter operations."
  (:require [twitter.oauth :refer [make-oauth-creds]]
            [twitter.api.restful :refer [users-show
                                         statuses-user-timeline]]))

(def ^:private creds
  (make-oauth-creds
    "htDShW8DFBzHtsSEsxWwLFHUc"
    "GpYTdiAPWFod2JkmCwYoedefyjvuvoJaB5ERapbDKron5lP8RB"
    "191624404-T9JVGKuqkfgcChzN7YJtbjO7kcoagsAHVqDgmXFh"
    "tiIebSK1Sn3lbcQmDd6AMP0JUaoIvhUakL0jfB8vrFrIf"))

(defn- timeline*
  "Fetches a single page of tweets from a user's timeline."
  [opts]
  (statuses-user-timeline
    :oauth-creds creds
    :params (merge {:screen-name "lubbockonline"
                    :count 100}
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
  (map :text (timeline 10 {:count 10}))
  (clojure.pprint/pprint (timeline 10))
  (count (timeline 10 {:count 10})))
