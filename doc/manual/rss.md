# RSS/Atom Feed

Weepinbell generates [Atom](https://validator.w3.org/feed/docs/atom.html) feed for each domain.

Currently, the only option available is length of feed. If set to `n`, Atom feed lists only the newest `n` webmentions. Cannot use `0` and cannot list all available webmentions.

```edn
 :feed-length 8
```

