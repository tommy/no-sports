(ns no-sports.data
  (:require
    [clojure.edn :as edn]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [no-sports.util :as u]))

(let [indices (zipmap [:id :grade :text :url] (range))]
  (defn- el
    "Lookup a property of the data row by keyword."
    [k row]
    {:pre [(contains? indices k)]}
    (get row (indices k))))

(defn all-tokens
  "Returns a set of all tokens in the entire dataset"
  ([a b & ds]
   (apply into (map all-tokens (conj ds a b))))
  ([dataset]
   {:pre [(and (map? dataset) (string? (key (first dataset))))]}
   (reduce
     (fn [acc text] (into acc (u/tokenize text)))
     (sorted-set)
     (keys dataset))))

(defn- load-dataset
  "Load a csv file named by the argument (default: training dataset)."
  ([]
   (load-dataset "training.csv"))
  ([n]
   (-> n io/resource io/reader csv/read-csv)))

(defn- to-map
  "Convert the tabular dataset into a map, where the key is the set of
  (stemmed) tokens in the tweet text, and the value is the grade."
  [dataset]
  {:pre [(every? #{4} (map count dataset))]}
  (reduce
    (fn [acc row] (assoc acc (el :text row) (el :grade row)))
    {}
    dataset))

(defn load-data
  [& args]
  (to-map (apply load-dataset args)))

(defn load-edn
  [n]
  (-> n io/resource slurp edn/read-string))
