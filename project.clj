(defproject no-sports "0.1.1-SNAPSHOT"
  :description "Bot that retweets @lubbockonline articles that don't have to do with sports."
  :url "http://twitter.com/NoSportsAJ"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.6.6"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [clj-http "0.9.1"]
                 [twitter-api "0.7.8"]
                 [clj-oauth "1.5.1"]
                 [org.clojure/data.csv "0.1.2"]
                 [cheshire "5.3.1"]
                 [clj-tokenizer "0.1.0"]
                 [com.nuroko/nurokit "0.0.3"]
                 [com.nuroko/nurokore "0.0.6"]
                 [judgr "0.3.0"]
                 [net.mikera/vectorz "0.13.1"]]

  :main no-sports.core

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:unchecked"]

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[clj-http-fake "1.0.3"]]}})
