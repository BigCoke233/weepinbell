# Webhook

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
