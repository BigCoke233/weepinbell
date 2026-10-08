# bellsprout

Bellsprout is a HTML + CSS + [Squint](https://github.com/squint-cljs/squint) SPA that is meant to work with Weepinbell. Squint compiles to vanilla JavaScript, so this is essentially a frameworkless frontend app. 

## Features 

- A form that sends webmention given source and target 
- Query webmentions and display them as cards

## Setup

You need Babashka and NPM to get started.

To install JavaScript dependencies:

    npm install

To watch for file changes and rebuild automatically:

    bb watch

To create a production build run:

    bb build

And open your browser in `resources/public/index.html`.

## License

Bellsprout is free software licensed under the GNU Affero General Public License.

