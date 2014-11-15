(ns no-sports.data
  (:require [clojure.string :as s]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(let [indices (zipmap [:id :grade :text :url] (range))]
  (defn el
    "Lookup a property of the data row by keyword."
    [k row]
    {:pre [(contains? indices k)]}
    (get row (indices k))))

(defn token-set
  "Given a row from the dataset, returns a set of all tokens of the tweet text."
  [row]
  (as-> row ?
    (if (coll? ?) (el :text ?) ?)
    (s/split ? #"\s+")
    (into #{} ?)))

(defn all-tokens
  "Returns a set of all tokens in the entire dataset"
  [dataset]
  (reduce
    (fn [acc row] (into acc (token-set row)))
    (sorted-set)
    dataset))

(defn load-dataset
  "Load a csv file named by the argument (default: training dataset)."
  ([]
   (load-dataset "training.csv"))
  ([n]
   (-> n io/resource io/reader csv/read-csv)))

(defn to-map
  [dataset]
  (reduce
    (fn [acc row] (assoc acc (token-set row) (el :grade row)))
    {}
    dataset))
