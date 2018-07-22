(defproject no-sports "0.1.1-SNAPSHOT"
  :description "Bot that retweets @lubbockonline articles that don't have to do with sports."
  :url "http://twitter.com/NoSportsAJ"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [org.clojure/core.async "0.4.474"]
   [org.clojure/tools.logging "0.4.1"]
   [org.slf4j/slf4j-log4j12 "1.7.25"]
   [org.apache.logging.log4j/log4j-core "2.11.0"]
   [clj-http "3.9.0"]
   [clj-oauth "1.5.5"]
   [org.clojure/data.csv "0.1.4"]
   [cheshire "5.8.0"]
   [clj-tokenizer "0.1.0"]
   [judgr "0.3.0"]]

  :main no-sports.core

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:unchecked"]

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]

  :profiles
  {:uberjar {:aot :all}
   :dev {:dependencies [[clj-http-fake "1.0.3"]]}
   ;; profile that includes a neural net-based classifer
   :neural {:source-paths ["neural-src/main/clojure"]
            :test-paths ["neural-src/test/clojure"]
            :java-source-paths ["neural-src/main/java"]
            :dependencies [[com.nuroko/nurokit "0.0.3"]
                           [com.nuroko/nurokore "0.0.6"]
                           [net.mikera/vectorz "0.13.1"]]}})
