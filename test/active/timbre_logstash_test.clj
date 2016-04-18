(ns active.timbre-logstash-test
  (:require [active.timbre-logstash :refer :all]
            [taoensso.timbre :as timbre]
            [clojure.test :refer :all]))

;; to test data->json-string
(def m1
  {:config {:foo :bar}
   :appender-id "foo"
   :appender {:baz :bla}
   :instant :foo
   :level :warning
   :error-level? :fatal
   :?ns-str "ns"
   :?file nil
   :?line 15
   :?err_ (delay (Exception. "foo"))
   :hostname_ (delay "hostname")
   :msg_ (delay "msg")
   :timestamp_ (delay "2015-07-17T09:01:45.539Z")
   :output-fn (fn [foo] foo)
   :profile-stats :profile-stats})


(defn configure-logstash!
  [host port]
  (timbre/merge-config! {:appenders 
                         {:logstash
                          (timbre-json-appender host port)}}))

