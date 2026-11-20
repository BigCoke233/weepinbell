# ADR: In-memory caching strategies that reduce database query

Date: 2026-11-18

## Context

Every API query resulting in a database query is not performant. Frequently queryed data should be cached in memory. This will reduce database query, so server with slow disk I/O and those handling large traffic can perform better.

## Decision

### Strategy

Use `clojure/core.cache` to cache database queries.

- Every database query will be cached in memory.
- If cache hit, use in-memory copy instead of making a database query.
- When updated, delete cache key so next query will hit database and retrieve the latest data.

The strategy would be LUCache (or Least Frequently Used), which means there would be a cache storage with limited space, when exceeding space limit, the least frequently used cache item will be deleted.

### Cache bucket

Currently there are two types of query:

- `get` (one specific webmention)
- `get-by-domain` (a list of webmentions, filtered by domain and, optionally, path)

There should be one cache bucket for each of them.

And the cache keys could be:

- ones bucket: `sha256(source+target)` or `id`
- list bucket: `sha256(domain+path)`

### Eviction

To keep cache consistent with data store, old data in the cache needs to be evicted when data store is updated (after appending, updating and deleting).

The tricky part is that a webmention can have two copies in both `ones` bucket and `list` bucket, and it's easy to evict cache in `ones` but complex to evict `list`, because you cannot get `sha256(domain+path)` from a single webmention.

To avoid unnecessary complexity, **empty `list` bucket** when data store is updated, and update `ones` bucket accordingly.

### Coding pattern

The Decorator pattern is a good fit. We already have `weepinbell.storage.worker` that wraps base storage to add asynchronous writing ability, it would be sensible to have `weepinbell.storage.cache` that adds cached reading ability. 

Basically, something like this:

```clojure
(worker/start (cache bare-store))
```

