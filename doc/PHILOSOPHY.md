# Philosophy

This document explains what weepinbell is, what it's not, and the design principles it follows. This document is for anyone who wants to get involved in the development of weepinbell and users who are simply curious. 

---

## Scope

### What It Is 

A tool for hosting [webmention](https://www.w3.org/TR/webmention/) service that receives and sends[^1] webmentions. It aims to be an all-in-one webmention toolkit that's easy to self-host. It's built for the [IndieWeb](https://indieweb.org/) community. It should be compatible with popular IndieWeb standards like [microformats](https://microformats.org/).

It should be capable of:

1. Receiving webmentions for one or many webmasters[^2]
2. Parsing structural data (e.g. microformats) of web pages that send webmentions 
3. Basic CRUD of webmentions and their metadata
4. Sending webmentions for anyone
5. Notifiying users about webmentions they receive or send

### What It's Not 

1. Full website interaction layer
2. Commenting system 
3. Tracking and analytics system 
4. Commercial or enterprise-level system 
5. LLM-powered application

---

## Architectural Decisions

### Minimal core

Weepinbell deliberately does **not** do a lot of things.

1. Weepinbell itself is headless and runs without a frontend.
2. Weepinbell does not store user credentials nor manage user groups or roles.

See weepinbell as a "micro-service".

- If you want a dashboard or any user interface, put a frontned in front of it.
- If you want multi-tenant features, develop a user management system that talks to weepinbell through its API, adding items to allowlist and etc.
- If you want a CLI, develop one that talks to its API.
- If you want none of that, great! Enjoy unbloated[^4] software!

### Scalability

Weepinbell is for both individuals and large groups.

1. It is small enough for individual to deploy with ease.
2. It is capable of handling hundreds of concurrent requests.

### Subsystems

Weepinbell is devided into small focused subsystems.

1. Web server, that handles API query and receives webmention requests.
2. Processor, that starts pipelines to process received webmentions.
3. Storage worker, that manipulates persistent data safely.

Subsystems are decoupled. They do not talk to each other directly. They put and take data to and from channels, just like standalone systems gets input and outputs.

See more in [Architecture](./ARCHITECTURE.md)

### Programming language, Clojure

Weepinbell is developed in Clojure, a Lisp that runs on the JVM. 

1. Clojure is a lazy functional language, which makes systems easier to maintain.
2. Clojure runs on JVM, a stable platform that runs on all major operating systems.
3. Clojure's semantics is pretty dense and it's easy to write less code and achieve more.
4. *It's a Lisp and I like it.*

Clojure has its downsides, which I decided to tolerate.

1. No native binaries. Users must install Java to run weepinbell. Though compiling to native executables using GraalVM is possible, it take extra effort and the binary size is not what you typically get from a compiled language, like Go, Rust or Zig.
2. Small community. Though clojure has a vibrant community, it's hard to find contributors who are both interested in IndieWeb and Lisp. Development can be slow with only one or two active contributors.

---

## Why the name "weepinbell"

Webmention is basically notification to someone when someone else mentioned them, which is like ringing a bell. And it's a name of a Pokemon. No particular reaon about why it has to be a pokemon.

[^1]: Sender feature is planned but not yet implemented. See [#24](https://codeberg.org/eltrac/weepinbell/issues/24).
[^2]: Webmasters refers to those who own a website.
[^3]: Trust me. 100 LoC is more than enough for Clojure.
[^4]: One might argue Clojure/Java itslef it not "unbloated" enough. Fair point, but I'm doing the best I can.
