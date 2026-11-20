# Performance

Weepinbell is quite scalable and there are a few config options you can tweak to make weepinbell perform better specifically to your needs.

## Pipeline Concurrency

> [!note]
> This feature is introduced in `v1.0.0` release.

When a webmention is received, they are put into a queue and then a processor will pick it up and process it (verify if `source` really mentioned `target`, extract metadata, write to storage...).

If you're hosting weepinbell for yourself or a small community, chances are, you don't receive many webmentions per day. So one processor would be more than enough. However, if you're hosting a public instance that might have to process a lot of data, you'll want to increase concurrency.

In `config.edn`:

```edn
 :pipeline-concurrency 5
```

The number here means the amount of processors that runs concurrently. The more there is, the more webmentions they can handle at the same time (also more RAM and CPU usage).

## Query Paging

> [!note]
> The following configuration options are introduced in `v1.1.0` release.

`/api/mentions` returns a list of webmentions given certain parameters, which includes `page`. The idea is that the whole list is partitioned into several page frames and `/api/mentions?page=n` returns the `n`th frame.

If `page` parameter is missing, by default, weepinbell returns **the whole list without paging**. For small sites with little data, downloading the whole list is convenient. For those dealing with large traffic, you probably want to turn this off to save bandwidth:

```edn
 :allow-unpaged-query true ;; set to `false` to turn it off
```

When unpaged query is disallowed, query without `page` parameter returns the last page frame.

You can also set the size of each page frame:

```edn
 :query-page-size 12
```

## Cached Query

Each request to `/api/mentions` results in a database query (or file read) by default. Again, this is good enough for small sites. It keeps data consistent while consuming little RAM.

However if you run on a server with slow I/O or have to deal with frequent queries, you'll probably want to cache query results so repetitive queries can use data in memory without querying the database.

```edn
 :cache-query false ;; set to `true` to enable caching
```

Note that this feature is not very stable. If you're using it and have issues, please report it.

