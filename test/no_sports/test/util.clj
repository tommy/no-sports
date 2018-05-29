(ns no-sports.test.util
  (:require
    [clj-http.fake :as fake]))

(def expect-snitch
  {#"https://nosnch\.in/596c61db7e"
   (constantly {:status 200})})

(defn mocks-fixture
  [test-fn]
  (fake/with-global-fake-routes-in-isolation
    expect-snitch
    (test-fn)))
