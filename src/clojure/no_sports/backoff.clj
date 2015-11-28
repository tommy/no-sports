(ns no-sports.backoff)

(defn backoff
  "Construct data object to track backoff."
  [min-wait max-wait decay-time
   & {:keys [timestamp]
      :or {timestamp (System/currentTimeMillis)}}]
  {:min-wait min-wait
   :max-wait max-wait
   :decay-time decay-time
   :timestamp timestamp
   :attempt 0})

(defn calculate-wait
  "Calculates the next amount of time to wait. The value is
  min-wait * (2 ** n)
  subject to min and max wait times."
  [{:keys [attempt min-wait max-wait]}]
  (-> (Math/pow 2 attempt)
      (* min-wait)
      (min max-wait)
      (max min-wait)
      long))

(defn decay
  "Resets the attempts to zero if enough time has passed."
  [backoff timestamp]
  {:pre [timestamp]}
  (let [diff (- timestamp (:timestamp backoff))]
    (if (> diff (:decay-time backoff))
      (assoc backoff :attempt 0)
      backoff)))

(defn split
  "Branches the exponential backoff timer. Returns a tuple of
  [wait-time new-backoff]. Uses the current time to determine
  decay unless a timestamp is specified."
  ([backoff]
   (split backoff (System/currentTimeMillis)))
  ([backoff timestamp]
   (let [decayed-backoff (decay backoff timestamp)
         wait-time (calculate-wait decayed-backoff)
         new-backoff (-> decayed-backoff
                         (update-in [:attempt] inc)
                         (assoc :timestamp timestamp))]
     [wait-time new-backoff])))

