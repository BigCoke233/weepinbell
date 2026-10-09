# Configuration

## 0. Create a `.edn` file

> [!note]
> If you're not familiar with this format, see [edn](https://github.com/edn-format/edn). In short, it's Clojure's equivalent of JSON, but more extensible.

Configure weepinbell by creating `config.edn` at the app's working directory[^1].

Copy-paste the following [default configuration](https://codeberg.org/eltrac/weepinbell/src/branch/doc/resources/config.edn) to your file and start editing.

```edn
{:port "3000"
 :base-url "http://localhost:3000"
 :endpoint "/endpoint"
 :allowed-target-domains ["example.com"]
 :data-path "data/webmention.edn"
 :webhook {:on-receive []
           :on-create []
           :on-update []
           :on-delete []}
 :webhook-request-fn nil
 :feed-length 8}
```

Port can also be configured with enviroment variable `PORT`. Config overrides enviroment variable.

## 1. First options to modify 

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

## 2. Tweak some features

### Webhook

Weepinbell can be configured to send `POST` requests to certain URLs when certain events ocurred, like when a webmention is received (but not processed yet), created, updated or deleted.

```edn
 :webhook {:on-receive []
           :on-create ["https://send-to-me-when-created.com"]
           :on-update ["https://can-have-multiple-URLs-under-one.event"
                       "https://like-this.net"]
           :on-delete []}
```

If an event key's value is empty, request will not be sent when that event occurs.

Webhook requests can carry customized payload. You need a *payload generator function*. The function should take two arguments, `hook` and `data`. 

- `hook`: a Clojure keyword, which can only be one of the four keywords: `:on-receive`, `:on-create`, `:on-update` and `:on-delete`.
- `data`: a wrapped webmention map, much like what you'll get in `/api/mention/{id}` (see [API Referrence](./API.md)), but a Clojure map. Note that when `hook` is `:on-receive`, `data` only contains `webmention` but no `microformats` or `extra` fields.

The return value should be a map, which will be merged to HTTP request options. Example:

```edn 
{:body "I'm in request body"
 :headers {"Content-Type" "text/plain"}
 :post-params {"title" "Webmention received"}}
```

Bascially:

```edn 
(fn [hook data] 
  {:body ...
   :headers {}
   ;; more 
  })
```

For now you can only use a preset generator by setting `:webhook-request-fn` to a keyword. Currently available presets are `:ntfy` and `:bark`. (for push notification apps [ntfy](https://ntfy.sh/) and [Bark](https://bark.day.app/))

```edn 
 :webhook-request-fn :ntfy ;; or :bark
```

See examples [here](https://codeberg.org/eltrac/weepinbell/src/commit/0471b4017ccab10f92f140881aeafba9ceb0896f/src/weepinbell/webhook_presets.clj#L21-L33). Feel free to [contribute](../CONTRIBUTING.md) more presets!

### RSS/Atom Feed

Weepinbell generates [Atom](https://validator.w3.org/feed/docs/atom.html) feed for each domain.

Currently, the only option available is length of feed. If set to `n`, Atom feed lists only the newest `n` webmentions. Cannot use `0` and cannot list all available webmentions.

```edn
 :feed-length 8
```

[^1]: Working directory is where the starting command is run. For example, if you `cd` into `/home/yourname/weepinbell` and run `java -jar weepinbell-xxx.jar`, then your configuration file should be `/home/yourname/weepinbell/config.edn`. If you're using Docker or systemd, you can configure working directory in `docker-compose.yaml` and `.service` files.
