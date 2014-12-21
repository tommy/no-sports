(ns no-sports.data
  (:require [no-sports.util :refer [tokenize]]
            [clojure.tools.reader.edn :as edn]
            [clojure.string :as s]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

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
     (fn [acc text] (into acc (tokenize text)))
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

;;;;;;;;;;;
;; repl dev

(comment
  (def t (mapv vec (load-dataset "grading.csv")))
  (def tt (mapv #(vector (get % 0) (get % 1) (get % 4) (get % 3)) t))
  (with-open [out-file (io/writer "resources/third.new.csv")]
    (csv/write-csv out-file
                   tt
                   :quote? (constantly true)))

  (def tr (load-data "training.csv")))
