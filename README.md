> [!important]
> This Project is still under active development. API may change. There may be fatal bugs.
>
> But we're close to a stable release.

# weepinbell

<img src="doc/images/weepinbell.avif" align="right" width="350px" />

<p>
<img src="https://img.shields.io/badge/clojure-1.12.6-brightgreen?logo=clojure" alt="clojure version">
<!--COVERAGE_BADGE:START--><img src="https://img.shields.io/badge/coverage-85.06%25-green" alt="test coverage 85.06%"><!--COVERAGE_BADGE:END-->
<a href="https://ci.codeberg.org/repos/16777" target="_blank">
  <img src="https://ci.codeberg.org/api/badges/16777/status.svg?workflow=test" alt="status-badge" />
</a>
</p>

> JESSIE: A Poké Ball! Yours as well?
>
> JAMES: And how! The very Pokémon I used to play with when we came to Sinnoh
>
> JAMES: Come on out, Carnivine!
>
> CARNIVINE: [appearing, bites James on the head]
>
> JAMES: You see? *Victreebel*'s not the only Pokémon who likes to take a nip at my noggin[^1]
>
> - Pokémon S10E2, Two Degrees of Separation!

Weepinbell is for handling [Webmentions](https://www.w3.org/TR/webmention/) and making decentralized communication easy for your indie websites.

## Features

- Implements the [Webmention](https://www.w3.org/TR/webmention/) protocol and more [IndieWeb](http://indieweb.org/) standards
- Translates [microformats](https://microformats.org/), [Open Graph](https://ogp.me/) and [JSON-LD](https://json-ld.org/) to [JF2](https://jf2.spec.indieweb.org/) for **structural data**, compatible with [webmention.io](https://webmention.io/)
- **Webhook** for automated workflows, with built-in support for [ntfy](https://ntfy.sh/) and [bark](https://bark.day.app/)
- **RSS feed** for each and every domain, so you can receive Webmentions from your reader
- Embeded [nREPL](http://nrepl.org/) server that evaluates code on the fly. *Ready for a [NASA-style](https://gigamonkeys.com/book/lather-rinse-repeat-a-tour-of-the-repl.html#:~:text=Debugging%20a%20program%20running%20on%20a%20%24100M%20piece%20of%20hardware%20that%20is%20100%20million%20miles%20away%20is%20an%20interesting%20experience.%20Having%20a%20read%2Deval%2Dprint%20loop%20running%20on%20the%20spacecraft%20proved%20invaluable%20in%20finding%20and%20fixing%20the%20problem.) remote debugging?*
- Image proxy for hosting remote images (e.g. author photo) on your own domain
- JSON API used for querying exisiting mentions 

## Documentation

- [Philosophy](./doc/PHILOSOPHY.md)
- **[User Manual](./doc/manual/index.md)**
- [Contributors' Guide](./doc/CONTRIBUTING.md)

## License

Weepinbell is free software licensed under the GNU Affero General Public License, with [additional permissions](https://codeberg.org/eltrac/weepinbell/commit/3cb62b1da148bce23785feeab95e75d1421090df) to EPL-licensed dependencies.

[^1]: *Victreebel* is *weepinbell* evolved. In the TV series, James' victreebel is a very affectionate Pokémon and likes to bite its trainer's head since when it was a *weepinbell*. The quote at the top of README is kind of a tradition in the Clojure community.
