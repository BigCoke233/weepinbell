# Migration

## Migrating from an older version

### Migrate from before `v1.0.0`

Before `v1.0.0`, weepinbell stored microformats rather than JF2, and did not support H2 database.

To convert microformats in your `.edn` file, back up your data and run this:

```shell
# change .jar name to what you have; use absolute paths
java -jar weepinbell-v1.x.x-standalone.jar edn-jf2ify /path/to/old.edn /path/to/new.edn

# or, if you have Leiningen installed, you can run this in the top directory of the repository
lein run edn-jf2ify /path/to/old.edn /path/to/new.edn
```

To migrate from EDN file-based storage to H2 database, run this (it automatically converts microformats to JF2 as well):

```shell
java -jar weepinbell-v1.x.x-standalone.jar edn-jf2ify /path/to/old.edn /path/to/database

# or
lein run edn-jf2ify /path/to/old.edn /path/to/new.edn
```

Also see [storage](./storage.md).

## Migrating from another service

### `webmention.io`

You can migrate from [webmention.io](https://webmention.io/) with [this Babashka script](script/migrate-from-webmentionio.clj). Make sure you have [Babashka](https://babashka.org/) installed and:

```shell
./script/migrate-from-webmentionio.clj <token> <new_endpoint>
```

Get `<token>` in [webmention.io settings](https://webmention.io/settings). `<new_endpoint>` is your current Weepinbell endpoint (default: `localhost:3000/endpoint`).

This script basically just resends all webmentions to Weepinbell. To avoid being mistaken as attacks, webmentions are resent every 0.5 seconds, so it might take a long time.

> [!note]
> We're developing a Batch API to simplify the process. See [#38](https://codeberg.org/eltrac/weepinbell/issues/38). Contributions are greatly appreciated!

