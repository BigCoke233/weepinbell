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

## 2. Configure the app

Create a file named `config.edn` at the current working directory[^1], or set environment variable `WEEPINBELL_CONFIG` to a custom file pathname. Either way, it should be written in EDN format.

> [!note]
> If you're not familiar with this format, see [edn](https://github.com/edn-format/edn). In short, it's Clojure's equivalent of JSON, but more extensible.

Use [resources/config.edn](../../resources/config.edn) as a template.

Refer to comments in the template config file or a feature's documentation for details. But first, you should change at least the following two options in your config file.

For weepinbell to receive webmentions for your site, add your site's domain to `:allowed-target-domains`.

```edn 
 :allowed-target-domains ["your.site.domain" "can-have.more-than.one"]
```

For weepinbell to return the right URLs to webmentions and proxied photos, set `:base-url` to where you're hosting weepinbell, **without trailing slash**.

```edn 
 :base-url "https://example.com"
``` 

> [!warning]
> You have to **restart** weepinbell for configuration to take effects.

You may want to know how and where your webmentions are stored by default and how to change it, see [storage](./storage.md).

## 3. Run the app

### The command 

```shell
java -jar weepinbell-v0.x.x-standalone.jar
```

When you see this line of log, you're good to go: 

``` 
2026-09-28T08:20:45.680Z MACHINE INFO [weepinbell.core:43] - weepinbell started, listening on 3000
```

> [!warning]
> If you're just trying weepinbell, running it with a command is fine. It's recommended to use a service manager or a container. See below.

### `rc` (OpenBSD)

If you're running weepinbell on OpenBSD, it's recommended to use `rc` (stands for "Run Commands").

If you haven't already, install Java:

```sh
doas pkg_add jdk-25
```

Create a user named `_weepinbell`.

```sh
doas useradd _weepinbell
```

At `/home/_weepinbell`, place the `.jar` and create a shell script named `run.sh`:

```sh
#!/bin/ksh
exec /usr/local/jdk-25/bin/java -jar /home/_weepinbell/weepinbell-v1.x.x-standalone.jar >> /var/log/weepinbell.log 2>&1
```

Then, create `/etc/rc.d/weepinbell`:

```sh
#!/bin/ksh
daemon="/home/_weepinbell/run.sh"
daemon_user="_weepinbell"

pexp=".*weepinbell.*"

. /etc/rc.d/rc.subr

rc_bg=YES

rc_cmd $1
```

Enable and run:

```sh
doas rcctl enable weepinbell
doas rcctl start weepinbell
```

To stop weepinbell:

```sh
doas rcctl stop weepinbell
```

### `systemd` (Linux)

If you haven't already, install Java with your package manager.

Then, create a user named `weepinbell`:

```sh
sudo useradd weepinbell
```

Place the `.jar` at `/home/weepinbell/`.

Create `/etc/systemd/system/weepinbell.service`:

```systemd
[Unit]
Description=Weepinbell
After=network.target

[Service]
Type=simple
User=weepinbell
Group=weepinbell
Environment="PORT=3000"
WorkingDirectory=/home/eltrac/weepinbell
ExecStart=/usr/bin/java -jar /home/eltrac/weepinbell/weepinbell-v1.x.x-standalone.jar
SuccessExitStatus=143
Restart=always

[Install]
WantedBy=multi-user.target
```

Reload daemon, enable service and run:

```sh
sudo systemctl daemon-reload
sudo systemctl enable weepinbell
sudo systemctl start weepinbell
```

To stop weepinbell:

```sh
sudo systemctl stop weepinbell
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

## 4. Set up web server

It's recommended to put weepinbell behind a reverse proxy, using something like [nginx](https://nginx.org/) and [caddy](https://caddyserver.com/). Here's an example `Caddyfile`:

```Caddyfile 
example.com 

reverse_proxy :3000
```

Weepinbell listens port `:3000` by default. Edit configuration `:port` if you want something else.

## 5. Point your site to weepinbell 

Add this to your `<head>`:

```html 
<link rel="webmention" href="https://example.com/endpoint">
```

Senders will look for this `<link>` tag and know where to send webmentions to.

[^1]: Working directory is where the starting command is run. For example, if you `cd` into `/home/yourname/weepinbell` and run `java -jar weepinbell-xxx.jar`, then your configuration file should be `/home/yourname/weepinbell/config.edn`. If you're using Docker or systemd, you can configure working directory in `docker-compose.yaml` and `.service` files.
