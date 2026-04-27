> [!warning]
> This Project is still under active development. API may change. There may be fatal bugs.

<img src="resources/static/weepinbell.avif" align="right" width="300px" />

# weepinbell

![pipeline status](https://ci.codeberg.org/api/badges/16777/status.svg)

> "When hungry, it swallows anything that moves. Its hapless prey is dissolved by strong acids."

A feature-rich [Webmention](https://www.w3.org/TR/webmention/) receiver written in [Clojure](https://clojure.org/), with [microformats](https://microformats.org/) and RSS support. 

It *eats up* in-coming Webmention request, *dissolve* the target's HTML and *grows* its own data file. And it rings its little *bell* to let you know that people mentioned you. Such a lovely pokémon!

## Features

- Implements [Webmention](https://www.w3.org/TR/webmention/) receiver
- Parse and store [microformats](https://microformats.org/)
- JSON API used for querying exisiting mentions 
- RSS Feed 
- Webhook

## Roadmap

- [ ] Unit tests (100% coverage) 🚩
- [ ] API Paging
- [ ] Admin UI 

## API

### Receiver Endpoint

`POST /endpoint`

You can configure this endpoint path. There are not separate endpoints for different domains. All Webmentions are sent to one endpoint.

### Query and filter Webmentions

`GET /api/mentions`

Respond with a JSON array containing all webmentions by default. Accepts query `filter`, either a target URL or a domain name, e.g. `/api/mentions?filter=example.com`.

`mentions` is for generic Webmention. If a Webmention target has microformats markup, it may contain `u-like-of` `u-in-reply-to` and `u-repost-of` elements, Weepinbell see them as special mentions. Use `/api/likes` `/api/replies` and `/api/reposts` to get these special mentions.

### Query one specific Webmention

`GET /api/mention/:id`

`GET /api/mention/?target=...&source=...`

Special mentions `/api/{likes|replies|reposts}` works, too.

### RSS Feed 

`/feed/:domain`

Subscribe to Webmentions sent to a specific domain.

## Installation

### Requirements

- JDK (Java), >= 17
- [Leiningen](https://codeberg.org/leiningen/leiningen) (optional, only if you want to build from source)

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

Podman should work as well, but it's not tested.

## Configuration

Configure weepinbell by creating a `config.edn`, the defaults:

```edn
{:base-url "https://localhost:3000"
 :endpoint "/endpoint" ;; POST route that receives webmention request
 :allowed-target-domains ["example.com"] ;; domains that this receiver receives webmention on behalf of
 :data-path "data/webmention.edn"
 :webhook {:on-receive [] ;; sends GET requests to these URLs
           :on-create []  ;; when certain events occur
           :on-delete []
           :on-update []}
 :feed-length 8 ;; number of RSS feed items
}
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

