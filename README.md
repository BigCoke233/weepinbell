> [!warning]
> This Project is still under active development. API may change. There may be fatal bugs.

<img src="doc/images/weepinbell.avif" align="right" width="300px" />

# weepinbell

<p>
<img src="https://img.shields.io/badge/clojure-1.12.6-brightgreen?logo=clojure" alt="clojure version">
<!--COVERAGE_BADGE:START--><img src="https://img.shields.io/badge/coverage-79.32%25-green" alt="test coverage 79.32%"><!--COVERAGE_BADGE:END-->
<a href="https://ci.codeberg.org/repos/16777" target="_blank">
  <img src="https://ci.codeberg.org/api/badges/16777/status.svg?workflow=test" alt="status-badge" />
</a>
</p>

> "When hungry, it swallows anything that moves. Its hapless prey is dissolved by strong acids."

A feature-rich [Webmention](https://www.w3.org/TR/webmention/) receiver written in [Clojure](https://clojure.org/), with [microformats](https://microformats.org/) and RSS support. 

It *eats up* in-coming Webmention request, *dissolve* the target's HTML and *grows* its own data file. And it rings its little *bell* to let you know that people mentioned you. Such a lovely pokémon!

## Features

- Implements [Webmention](https://www.w3.org/TR/webmention/) receiver
- Parse and store [microformats](https://microformats.org/)
- JSON API used for querying exisiting mentions 
- RSS Feed 
- Webhook
- Image proxy for microformats photos

## Documentation

- [Philosophy](./doc/PHILOSOPHY.md)
- [User Manual](./doc/manual/index.md) ✋ (*Get started here!*)
- [Contributors' Guide](./doc/CONTRIBUTING.md)
- [Architecture](./doc/ARCHITECTURE.md)

## License

Weepinbell is free software licensed under the GNU Affero General Public License.

