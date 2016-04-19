(ns active.timbre-logstash
  "Timbre appenders that send output to logstash."
  (:require [taoensso.timbre :as timbre]
            [cheshire.core :as cheshire])
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

(def iso-format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

(defn data->json-stream
  [data writer opts]
  ;; Note: this it meant to target the logstash-filter-json; especially "message" and "@timestamp" get a special meaning there.
  (let [stacktrace-str (if-let [pr (:pr-stacktrace opts)]
                         #(with-out-str (pr %))
                         timbre/stacktrace)]
    (cheshire/generate-stream
     (merge (:context data)
            {:level (:level data)
             :?ns-str (:?ns-str data)
             :?file (:?file data)
             :?line (:?line data)
             :err (some-> (force (:?err_ data)) (stacktrace-str))
             :hostname (force (:hostname_ data))
             :message (force (:msg_ data))
             "@timestamp" (:instant data)})
     writer
     (merge {:date-format iso-format
             :pretty false}
            opts))))

(defn timbre-json-appender
  "Returns a Logstash appender."
  [host port & [opts]]
  (let [conn (atom nil)
        nl "\n"]
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
                                       (connect host port))))]
           (locking sock
             (data->json-stream data out (select-keys opts [:pr-stacktrace]))
             ;; logstash tcp input plugin: "each event is assumed to be one line of text".
             (.write ^java.io.Writer out nl)))
         (catch java.io.IOException _
           nil)))}))
