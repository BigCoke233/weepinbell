# bellsprout

Bellsprout is a HTML + CSS + ClojureSript SPA that is meant to work with Weepinbell.

## Features 

- A form that sends webmention given source and target 
- Query webmentions and display them as cards

## Setup

You need Leiningen, Babashka and NPM to get started.

To get an interactive development environment run:

    lein figwheel

To create a production build run:

    bb build

And open your browser in `resources/public/index.html`.

## License

Bellsprout is free software licensed under the GNU Affero General Public License.

