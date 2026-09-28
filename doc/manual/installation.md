# Installation

## 0. Requirements

- JDK (Java), >= 17
- [Leiningen](https://codeberg.org/leiningen/leiningen) (optional, only if you want to build from source)

## 1. Get the `.jar`

### Download pre-built jar files

You can download the latest version in [Releases](https://codeberg.org/eltrac/weepinbell/releases) page.

```shell 
# or via command line
wget https://codeberg.org/eltrac/weepinbell/releases/download/vx.x.x/weepinbell-vx.x.x-standalone.jar
```

> [!note] 
>
> **What's the difference between a `standalone` jar and a regular jar?**
> 
> You may have noticed that `standalone` jars are larger, that's because it has all the dependencies bundled in and can be run directly. Unless you want to manage dependencies manually, you should always use the `standalone` jar.

### Build from source

Make sure you have [Leinigen](https://leiningen.org/) installed.

```shell
git clone https://codeberg.org/eltrac/weepinbell
cd weepinbell
lein uberjar
```

And grab `target/weepinbell-v0.x.x-standalone.jar`.

## 2. Run the app

### The command 

```shell
java -jar weepinbell-v0.x.x-standalone.jar
```

When you see this line of log, you're good to go: 

``` 
2026-09-28T08:20:45.680Z MACHINE INFO [weepinbell.core:43] - weepinbell started, listening on 3000
```

### Docker

> [!warning]
> The `Dockerfile` is not actively tested.

[Dockerfile](Dockerfile) and [docker-compose.yml](docker-compose.yml) are included in the repository.

```shell
git clone https://codeberg.org/eltrac/weepinbell
cd weepinbell
docker compose up -d
```

Podman should work as well, but it's not tested.

## 3. Set up web server

It's recommended to put weepinbell behind a reverse proxy, using something like [nginx](https://nginx.org/) and [caddy](https://caddyserver.com/). Here's an example `Caddyfile`:

```Caddyfile 
example.com 

reverse_proxy :3000
```

Weepinbell listens port `:3000` by default. See [configuration](./configuration.md) on how to change it.

## 4. Point your site to weepinbell 

Add this to your `<head>`:

```html 
<link rel="webmention" href="https://example.com/endpoint">
```

Senders will look for this `<link>` tag and know where to send webmentions to.

---

Now weepinbell is installed on your site. Before it can actually receive webmentions for you, you need to [configure it](./configuration.md).

