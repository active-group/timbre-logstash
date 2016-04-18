(ns active.timbre-logstash
  "Timbre appenders that send output to logstash."
  (:require [taoensso.timbre :as timbre]
            [cheshire.core :as cheshire]
            [clj-time.coerce :as time-coerce]
            [clj-time.format :as time-format])
  (:import  [java.net Socket InetAddress]
            [java.io PrintWriter]))

;; Adapted from taoensso.timbre.appenders.3rd-party.server-socket

(defn connect
  [host port]
  (let [addr (InetAddress/getByName host)
        sock (Socket. addr (int port))]
    [sock
     (PrintWriter. (.getOutputStream sock))]))

(defn connection-ok?
  [[^Socket sock ^PrintWriter out]]
  (and (not (.isClosed sock))
       (.isConnected sock)
       (not (.checkError out))))

(def iso-formatter (time-format/formatters :date-time))

(defn data->json-string
  [data]
  ;; Note: this it meant to target the logstash-filter-json; especially "message" and "@timestamp" get a special meaning there.
  (cheshire/generate-string
   (merge (:context data)
          {:level (:level data)
           :error-level? (:error-level? data)
           :?ns-str (:?ns-str data)
           :?file (:?file data)
           :?line (:?line data)
           :err (some-> (force (:?err_ data)) timbre/stacktrace)
           :hostname (force (:hostname_ data))
           :message (force (:msg_ data))
           "@timestamp" (time-format/unparse iso-formatter (time-coerce/from-date (:instant data)))})))

(defn timbre-json-appender
  "Returns a Logstash appender."
  [host port]
  (let [conn (atom nil)]
    {:enabled?   true
     :async?     false
     :min-level  nil
     :rate-limit nil
     :output-fn  :inherit
     :fn
     (fn [data]
       (try
         (let [[sock out] (swap! conn
                                 (fn [conn]
                                   (or (and conn (connection-ok? conn) conn)
                                       (connect host port))))
               json (data->json-string data)]
           (binding [*out* out]
             (println json)))
         (catch java.io.IOException _
           nil)))}))
