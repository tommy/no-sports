(ns no-sports.data
  (:require [clojure.string :as s]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clj-tokenizer.core :as tok]))

(let [indices (zipmap [:id :grade :text :url] (range))]
  (defn el
    "Lookup a property of the data row by keyword."
    [k row]
    {:pre [(contains? indices k)]}
    (get row (indices k))))

(defn tokens
  [text]
  {:pre [(string? text)]}
  (-> text
    (s/split #"\s+")
    set))

(defn token-set
  "Given a row from the dataset, returns a set of all tokens of the tweet text."
  [row]
  {:pre [(vector? row)
         (#{2 5} (count row))]}
  (case (count row)
    5 (tokens (el :text row))
    2 (key row)))

(defn all-tokens
  "Returns a set of all tokens in the entire dataset"
  ([a b & ds]
   (apply into (map all-tokens (conj ds a b))))
  ([dataset]
   {:pre [(or
            (and (sequential? dataset) (sequential? (first dataset)))
            (and (map? dataset) (set? (key (first dataset)))))]}
   (reduce
     (fn [acc row] (into acc (token-set row)))
     (sorted-set)
     dataset)))

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

(defn load-data
  [& args]
  (to-map (apply load-dataset args)))


(comment
  (count (all-tokens (load-data "training.csv")))
  (count (all-tokens (load-data "grading.csv")))
  (count (apply all-tokens (map load-data ["training.csv"
                                           "grading.csv"]))))
