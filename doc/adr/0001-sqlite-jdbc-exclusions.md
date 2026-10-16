# ADR: Exclude some native SQLite libraries in `sqlite-jdbc`

Date: 2026-10-16

## Context

Weepinbell needs to support SQLite and the only way to do it is to use JDBC (Java DataBase Connectivity). The [SQLite JDBC Driver](https://github.com/xerial/sqlite-jdbc) is quite large (> 12MB), because it includes `.so` native libraries **for every platforms and architectures it supports in a single `.jar`**.

Bundling `sqlite-jdbc` inside weepinbell's uberjar makes it almost twice as big.

## Decision

Reasonbaly, web services like weepinbell does not run on macOS, Windows and Android. Therefore, I excluded these native libraries in uberjar.

```clojure
  :profiles {:uberjar {:aot :all
                       :uberjar-exclusions [#"^org/sqlite/native/mac/"
                                            #"^org/sqlite/native/windows/"
                                            #"^org/sqlite/native/linux-android/"]}
```

In theory, users can bring their own SQLite library to support these platforms.

The SQLite storage feature officially only supports Linux and FreeBSD.

