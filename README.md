# timbre-logstash

Send [Timbre](https://github.com/ptaoussanis/timbre) output to [Logstash](https://www.elastic.co/products/logstash).

## Usage

The function `timbre-json-appender` returns a Timbre appender, which
will connect to a server created with the [Logstash TCP input
plugin](https://www.elastic.co/guide/en/logstash/current/plugins-inputs-tcp.html),
and send all events in JSON format to it:

```clojure
(timbre-logstash/timbre-json-appender host port)
```

The returned appender is `enabled?`, not `async?`, has no `min-level`
and no `rate-limit` set. The `output-fn` setting is not used.

Note that the TCP input plugin of Logstash just puts each received line of text
into the `message` field of the Logstash event, so you probably also want
to expand the structured information in the JSON object with the [JSON
filter
plugin](https://www.elastic.co/guide/en/logstash/current/plugins-filters-json.html).

The JSON object sent to Logstash by this appender is constructed by
merging the Timbre context map with a map consisting of `:level`,
`:namespace`, `:file`, `:line`, `:stacktrace`, `:hostname` and of
course `:message` fields from the Timbre event data. The conversion of
EDN values to JSON data is done in the usual way, specifically via
[`cheshire.core/generate-stream`](https://github.com/dakrone/cheshire).
The instant in time of the event will also be added to the Logstash
event data as a field named `@timestamp`. This will be picked up by
the Logstash JSON filter and set as the time of the event, so you'll
get the time the event happened, instead of the time the event arrived
at the Logstash server.

## License

Copyright © 2015-2016 Active Group GmbH

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
