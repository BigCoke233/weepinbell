# Contributing to Weepinbell

Contributions are welcome and greatly appreciated. Get started here!

## First step

Go to [Issues](https://codeberg.org/eltrac/weepinbell/issues) and find ones labeled [contribution welcome](https://codeberg.org/eltrac/weepinbell/issues?q=&type=all&state=open&labels=2145577&milestone=0&assignee=0&poster=0), or [good first issue](https://codeberg.org/eltrac/weepinbell/issues?q=&type=all&state=open&labels=2145589&milestone=0&assignee=0&poster=0) if you're contributing for the first time. 

Once you find an issue you think you'll be able to help with, read the conversation and ask if anyone else has already been working on it. If not, you can ask the maintainer to assign you to that task and work on your fork. Good luck and thanks for contributing!

Don't create a PR if it hasn't been discussed and approved in an issue yet! If you have new ideas, please read [our philosophy](./PHILOSOPHY.md) to see if they align with our principles, and then open an issue.

## Style Guide 

### Code style 

1. Functions should be small and does one thing only, with clear or no side effects[^2].
2. A file should have less than 150 lines of code.[^1]
3. Test-Driven Development is not enforced, but committing tests alongside your code is encouraged.
4. Use utility functions and `->`/`->>` macros to reduce layers of indentation.
5. Format your code using [cljfmt](https://github.com/weavejester/cljfmt). (run `lein cljfmt fix`)

### :git: Commits and Pull Requests 

1. Use [Scoped Commits](https://scopedcommits.com/).
2. **Squash** your changes to a few large, standalone commits, and **rebase** `master` before you create a PR. This keeps the history clean.
3. Make sure you've run `lein test` and manually verified your changes before submitting PR. 
4. Keep description simple and concise. Highlight affected scopes, things other contributors should look out for.

## Principles

### Documentation first, code later 

1. A feature should be discussed in an issue before it's implemented in a PR.
2. When writing a function, write docstring first.
3. When designing a new component, write namespace's docstring first.
4. After a architecture decision, document the context and final decision in [ADR](./doc/adr/). (Also, read existing ADRs to understand why things are what they are)

> [!tip]
> Write and communicate in [Simple English](https://simple.wikipedia.org/wiki/Basic_English), so the project can be more inclusive of people who speak English as a second language and people with dyslexia.

### Minimal dependencies 

1. Never depend on things you don't need. For instance, if only one or two features of a library is used, we should not depend on that library, but should seek alternatives that's more focused, implement those features within our project, or copy-paste code we need exactly into our codebase.
2. If a requirement is not of great significance and implementing it will require introducing tons of dependencies, it's best to reconsider. Read [#19](https://codeberg.org/eltrac/weepinbell/issues/19) about how and why I abandoned the idea of auto-generating API documentation.

### Make things pluggable 

Whenever possible and necessary, pass something as a function argument instead of requiring it in that function's namespace, because:

1. It makes the function **testable**. It's easy to pass mock objects as arguments. It's difficult to redefine a namespace's dependencies.
2. It makes it easier to replace concrete implementation. If a component depends on SQL directly, it's hard to switch to KV storage. If a component uses something like `((:get-data storage) data-id)`, all we need to do is replace that `storage` vtable.
3. It's lighter than implementing interfaces or protocols. We don't need to create an object or a record. We only need a regular symbol or a collection of symbols.
4. It can still be type-safe if we check it using spec or schema.
5. It makes the system easier to maintain.

### Keep everything small and focused

Every part of the system should be small. A file should have no more than 150 lines of code.[^1] A function should be easy to understand and should not require knowledge of more than one layer of abstraction. The key to keep things small is to keep them clear. Everything should have a clear purpose and a clear way to achieve that purpose.

## Set up environment

Install Clojure, [Leinigen](https://leiningen.org/) and, optionally, [Babashka](https://babashka.org/).

To install dependencies:

```
lein deps 
```

To start REPL:

```
lein repl 
```

To start a dev server that automatically reloads namespaces on file changes (and an REPL): 

``` 
lein dev
```

To run project: 

``` 
lein run 
```

To compile: 

``` 
lein uberjar
```

Make sure to run our test suite and solve bugs captured by tests before you commit and create a PR. To run tests: 

``` 
lein test
```

## Generated Code Policy 

We do not accept generated code, for the same reasons as Clojure:

> Clojure’s code is written and reviewed by humans. Code generated by a large language model or similar technology, such as Anthropic’s Claude, GitHub/Microsoft’s Copilot, OpenAI’s ChatGPT, Facebook/Meta’s Code Llama et al, is not compliant with the covenants and representations of Clojure’s Contributor’s Agreement, and is thus not acceptable as code for Clojure.
>
> An exception may be made for human written generators whose source code is contributed along with the patch.

*from [Clojure's Contributor Agreement](https://clojure.org/dev/contributor_agreement#_no_generated_code)*

Also, refer to Codeberg's [Terms of Use](https://codeberg.org/Codeberg/org/commit/96fac426a32d1ba91ff879366d59bf1af54080c2):

> 7. You must not share projects that mostly consist of code written by "generative AI"-tools (including services such as *Claude*, *OpenAI Codex*). Such projects having an unclear copyright status (see requirements § 2 (1) 1 and § 2 (1) 3) and furthermore have little safeguards to ensure that they do not include harmful code (c.f. § 2 (1) 5).

Generated code, especially by an LLM, is not the prompter's to license. Such code may not be compatible with our GNU Affero General Public License. 

The following usage of LLMs is tolerated but not appreciated:

1. Using LLMs to review your code and find bugs (not to fix bugs for you).
2. Using LLMs to gather information about a topic or technology you're not familiar with.
3. Using LLMs to propose a solution to your problem, but not to be accepted blindly nor to have them implemented by an "agent".

The following usage of LLMs is strictly forbidden:

1. Writing documentation, issues and PR descriptions.
2. Generating code that has not been significantly modified by the contributors themselves. (By "significantly", I mean "significantly enough to claim copyright of this piece of code")
3. Spread information generated by an LLM without verifying it yourself.

## Licensing

By contributing to weepinbell, you agree to license your code under GNU Affero General Public License.

[^1]: It should be more than enough for Clojure. If not, consider refactoring.
[^2]: See *Functional Programming*
