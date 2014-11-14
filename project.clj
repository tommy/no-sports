(defproject no-sports "0.1.0-SNAPSHOT"
  :description "Bot that retweets @lubbockonline articles that don't have to do with sports."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [twitter-api "0.7.7"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-tokenizer "0.1.0"]
                 [com.nuroko/nurokit "0.0.3"]
                 [com.nuroko/nurokore "0.0.6"]
                 [net.mikera/vectorz "0.13.1"]]
  
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"])
