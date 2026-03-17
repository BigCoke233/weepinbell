```
⚠️ This Project is still under active development ⚠️
```

<img src="resources/static/weepinbell.avif" align="right" width="300px" />

# weepinbell

A [Webmention](https://www.w3.org/TR/webmention/) receiver written in [Clojure](https://clojure.org/).

## Features

- Implements [Webmention](https://www.w3.org/TR/webmention/) receiver
- Parse and store [microformats](https://microformats.org/)
- Webmention query API and filtering by target url or domain

## Roadmap

- [ ] API Paging
- [x] Generate unique id for each Webmention
- [ ] RSS/Atom Feed, see [Serving RSS with Clojure](https://yogthos.net/posts/2012-08-04-Serving-RSS-with-Clojure.html)
- [x] Import from [webmention.io](https://webmention.io)
- [x] Webhook

## API

### Receiver Endpoint

`POST /endpoint`

You can configure this endpoint path. There are not separate endpoints for different domains. All Webmentions are sent to one endpoint.

### Query and filter Webmentions

`GET /api/mentions`

Respond with a JSON array containing all webmentions by default. Accepts query `filter`, either a target URL or a domain name, e.g. `/api/mentions?filter=example.com`.

### Query one specific Webmention

`GET /api/mention/:id`

`GET /api/mention/?target=...&source=...`

## Installation

### Download

Weepinbell is provided as a `.jar` file. You can download the latest version in [Releases](https://codeberg.org/eltrac/weepinbell/releases) page.

```shell
java -jar weepinbell-v0.x.x-standalone.jar
```

### Build from source

Make sure you have [Leinigen](https://leiningen.org/) installed.

```shell
git clone https://codeberg.org/eltrac/weepinbell
cd weepinbell
lein uberjar
java -jar target/weepinbell-v0.x.x-standalone.jar
```

### Docker

[Dockerfile](Dockerfile) and [docker-compose.yml](docker-compose.yml) are included in the repository.

```shell
git clone https://codeberg.org/eltrac/weepinbell
cd weepinbell
docker compose up -d
```

## Configuration

Configure weepinbell by creating a `config.edn`, the defaults:

```edn
{:base-url "https://localhost:3000"
 :endpoint "/endpoint" ;; POST route that receives webmention request
 :allowed-target-domains ["example.com"] ;; domains that this receiver receives webmention on behalf of
 :data-path "data/webmention.edn"}
```

If you're not familiar with this format, see [edn](https://github.com/edn-format/edn). In short, it's Clojure's equivalent of JSON, but more extensible.

Port can be configured with enviroment variable `PORT`.

## Migration

You can migrate from [webmention.io](https://webmention.io/) with [this Babashka script](migrate-from-webmentionio.clj). Make sure you have [Babashka](https://babashka.org/) installed and:

```shell
./migrate-from-webmentionio.clj <token> <new_endpoint>
```

Get `<token>` in [webmention.io settings](https://webmention.io/settings). `<new_endpoint>` is your current Weepinbell endpoint (default: `localhost:3000/endpoint`).

This script basically just resend all webmentions to Weepinbell. To avoid being mistaken as attacks, webmentions are resent every 0.5 seconds, so it might take a long time.

## License

MIT License, Copyright © 2026 Eltrac

