# Philosophy

This document explains what weepinbell is, what it's not, and the design principles it follows. This document is for anyone who wants to get involved in the development of weepinbell and users who are simply curious. 

## What It Is 

A tool for hosting [webmention](https://www.w3.org/TR/webmention/) service that receives and sends[^1] webmentions. It aims to be an all-in-one webmention toolkit that's easy to self-host. 

It should be capable of:

1. Receiving webmentions for one or many webmasters[^2]
2. Parsing metadata (microformats) of webmentions 
3. Storing and querying webmentions 
4. Sending webmentions 
5. Notifiying users about webmentions they receive or send 

## What It's Not 

1. Full website interaction layer
2. Commenting system 
3. Commercial user tracking system 
4. Enterprise-level system 
5. LLM-powered application
6. Complex system that handles large amount of concurrent requests

## Principles 

### Documentation first, code later 

1. A feature should be discussed in an issue before it's implemented in a PR.
2. When writing a function, write docstring first.
3. When designing a new component, write namespace's docstring first.

### Minimal dependencies 

1. Never depend on things you don't need. For instance, if only one or two features of a library is used, we should not depend on that library, but should seek alternatives that's more focused, implement those features within our project, or copy-paste code we need exactly into our codebase.
2. If a requirement is not of great significance and implementing it will require introducing tons of dependencies, it's best to reconsider. Read [#19](https://codeberg.org/eltrac/weepinbell/issues/19) and see how and why we abandoned auto-generating API documentation.

### Make things pluggable 

Whenever possible, pass something as a function argument instead of requiring it in that function's namespace, because:

1. It makes the function **testable**. It's easy to pass mock objects as arguments. It's difficult to redefine a namespace's dependencies.
2. It makes it easier to replace concrete implementation. If a component depends on SQL directly, it's hard to switch to KV storage. If a component uses something like `((:get-data storage) data-id)`, all we need to do is replace that `storage`.
3. It's lighter than implementing interfaces or protocols. We don't need to create an object or a record. We only need a regular symbol or a collection of symbols.
4. It can still be type-safe if we check it using spec or schema.
5. It makes the system easier to maintain.

### Keep everything small and focused

Every part of the system should be small. A file should have no more than 100 lines of code.[^3] A function should be easy to understand and should not require knowledge of more than one layer of abstraction. The key to keep things small is to keep them clear. Everything should have a clear purpose and a clear way to achieve that purpose.

## Why Clojure 

1. Clojure is a functional language with laziness, which makes systems easier to maintain.
2. Clojure runs on JVM, a stable platform.
3. Clojure's semantics is pretty dense and it's easy to write less code and achieve more.
4. *It's a Lisp and I like it.*

## Why the name "weepinbell"

Webmention is basically notification to someone when someone else mentioned them, which is like ringing a bell. And it's a name of a Pokemon. No particular reaon about why it has to be a pokemon.

[^1]: Sender feature is planned but not yet implemented. See [#24](https://codeberg.org/eltrac/weepinbell/issues/24).
[^2]: Webmasters refers to those who own a website.
[^3]: Trust me. 100 LoC is more than enough for Clojure.
