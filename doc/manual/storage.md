# Storage

Weepinbell currently supports 2 types of storage, both file-based.

## Configuration

Options related to data storage:

```edn
 :storage-type :edn ; or :h2
 :data-path "webmention.edn" ; or H2 database name
```

By default, weepinbell uses EDN storage and writes data to `data/webmention.edn`.

## EDN file-based storage

> [!note]
> If you're not familiar with this format, see [edn](https://github.com/edn-format/edn). In short, it's Clojure's equivalent of JSON, but more extensible.

When `:storage-type` is set to `:edn`, weepinbell writes to and reads from a `.edn` file, but more accurately, it's in "EDN lines" format, like JSONL. Every line is a valid EDN data. Every line is a webmention entry. Every entry is a map containing the following keys: `:webmention`, `:jf2`, `:extra`.

It's not recommended to edit data file directly by hand, but if you must, back up before you do. Also, it's considered good practice to set up a cron job to routinely back up data file.

If you don't have much data, using a single EDN would be enough, though the current implementation is not performant and very slow.

## H2 database

H2 is a file-based SQL database in pure Java. Like SQLite, it's lightweight and fast. Unlike SQLite, it does not need a native C library, so it's slower but more suitable for Java and Clojure.

When `:storage-type` is set to `:h2`, weepinbell uses a H2 database. The `:data-path` is the path to the database file but **without extension name**, so instead of `"data/webmention.db"`, you put `"data/webmention"`.

H2 database is more performant and faster, and it's lightweight enough.

