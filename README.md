```
⚠️ This Project is still under active development ⚠️
```

<img src="weepinbell.avif" align="right" width="300px" />

# weepinbell

A [Webmention](https://www.w3.org/TR/webmention/) receiver written in [Clojure](https://clojure.org/).

## Features

- Implements [Webmention](https://www.w3.org/TR/webmention/) receiver
- Parse and store [microformats](https://microformats.org/)
- Webmention query API and filtering by target url or domain

## Roadmap

- [] API Paging
- [] RSS/Atom Feed, see [Serving RSS with Clojure](https://yogthos.net/posts/2012-08-04-Serving-RSS-with-Clojure.html)
- [] Import from [webmention.io](https://webmention.io)

## API

### Receiver Endpoint

`POST /endpoint`

You can configure this endpoint path. There are not separate endpoints for different domains. All Webmention are sent to one endpoint.

### Query and filter Webmentions

`GET /api/mentions`

Respond with a JSON array containing all webmentions by default. Accepts query `filter`, either a target URL or a domain name, e.g. `/api/mentions?target=example.com`.

## Config

Configure weepinbell by creating a `config.edn`, the defaults:

```edn
{:endpoint "/endpoint"
 :allowed-target-domains ["example.com"]
 :data-path "data/webmention.edn"}
```

If you're not familiar with this format, see [edn](https://github.com/edn-format/edn). In short, it's Clojure's equivalent of JSON, but more extensible.

## License

MIT License, Copyright © 2026 Eltrac

