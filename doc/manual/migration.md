# Migration

## Migrating from `webmention.io`

You can migrate from [webmention.io](https://webmention.io/) with [this Babashka script](script/migrate-from-webmentionio.clj). Make sure you have [Babashka](https://babashka.org/) installed and:

```shell
./script/migrate-from-webmentionio.clj <token> <new_endpoint>
```

Get `<token>` in [webmention.io settings](https://webmention.io/settings). `<new_endpoint>` is your current Weepinbell endpoint (default: `localhost:3000/endpoint`).

This script basically just resends all webmentions to Weepinbell. To avoid being mistaken as attacks, webmentions are resent every 0.5 seconds, so it might take a long time.

> [!note]
> We're developing a Batch API to simplify the process. See [#38](https://codeberg.org/eltrac/weepinbell/issues/38). Contributions are greatly appreciated!

