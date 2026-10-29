# HTTP API Referrence

## Notations used in this page 

- `*` means a parameter is optional, otherwise it's required.
- `<xxx>` means it's an variable, which will be replaced in the actual response.
    - `<config:xxx>` means it's from user's configuration.
    - `<value1 | value2 | value3>` means it will be replaced with one of the values.
- `{xxx}` means it's an input, which needs to be replaced with actual value in requests.

---

## Receiver endpoint

Receiver endpoint is where people send their webmentions to. It's a route that accepts POST request with form parameters `source` and `target`.

| Method | Path                                                  |
| ------ | ----------------------------------------------------- |
| `POST` | `/endpoint` (can be configured using option `:endpoint`, see `config.edn`) |

| Post Params | Description                                                                                                                                               | Default |
| ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| `source`    | Where webmention is coming from. The page that mentioned `target`.                                                                                        | None    |
| `target`    | Where webmention is sent to. The page that was mentioned by `source`. Target's domain should be in the allow list of receiver. (allow list is `:allowed-target-domains` in `config.edn`) | None    |

The endpoint responds with status code `201` and a `Location` header pointing to the webmention. It'll take some time for weepinbell to verify, process and store webmention. Senders should check the `Location` URL a few minutes after.

An immediate response is made upon request:

```json 
{
    "status": "201",
    "message": "Webmention is accepted and pending for verification.",
    "url": "<config:base_url>/api/mention/<webmention_id>"
}
```

The receiver does basic checks. If there's something wrong, it reponds with status code `400` and returns a JSON body:

```json 
{
    "status": "400",
    "message": "<specifc_error_message>"
}
```

All possible messages and their meanings: 

- "Invalid URLs. Source and target have to be valid http(s) urls."
- "Source and target are identical."
- "Domain of target is not allowed. This receiver does not receive webmention on its behalf."

The message themselves should be informative enough.

---

## Query routes 

### Querying a specific webmention 

| Method | Path                                            |
| ------ | ----------------------------------------------- |
| `GET`  | `/api/mention/{id}`                              |
| `GET`  | `/api/mention/?source={source}&target={target}` |

An example response: 

```json 
{
  "webmention": {
    "source": "<source>",
    "target": "<target>",
    "id": "<32_digit_SHA_256_hash>",
    "modified": <timestamp_of_last_modified>
  },
  "jf2": {
    "type": "entry", // it's always "entry"
    "name": "Title of this entry",
    "url": "URL to this entry",
    "author": {
        "type": "card", // it's always "card"
        "name": "Name of author",
        "url": "URL to this author",
        "photo": "Photo URL" // optional
    },
    "published": "Date string of published date",
    "content": {
        "html": "<p>content of this entry</p>",
        "text": "content of this entry"
    },
    "summary": "Summary or description of this entry", // optional
    "wm-property": "mention", // or "like-of", "repost-of", "in-reply-to", "rsvp"
    // below are fields that only exists when the coresponding "wm-property" is present
    "like-of": "URL that this webmention represents a like of",
    "repost-of": "URL that this webmention represents a repost of",
    "in-reply-to": "URL that this webmention replies to",
    "rsvp": "URL to the event that this webmention represents an RSVP for"
  },
  "extra": {
    "proxied-photo": "<config:base_url>/avatars/<photo_path>"
  }
}
```

### Querying a list of webmentions 


| Method | Path                          | Description                            |
| ------ | ----------------------------- | -------------------------------------- |
| `GET`  | `/api/mentions/{domain}`      | Webmentions sent to a specific domain. |
| `GET`  | `/api/mentions/?url={target}` | Webmentions sent to a specific URL.    |

| Query Params    | Description                                                                                                                                                      | Default |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| `page`*         | Page number starting from `0`. **If no page number, weepinbell returns a full list.** If not a number (e.g. `last`, a string), weepinbell returns the last page. | None    |
| `jf2type`*       | Filters webmentions by their JF2 type (.i.e the `wm-property`). Available types are `like-of`, `repost-of`, `in-reply-to` and `rsvp`. If absent, no filter is applied.        | None    |

The responses are mostly the same as `/api/mention/{id}` except that there are multiple webmention items wrapped inside a JSON array.

### Couting a list of webmentions

| Method | Path                       | Description                                     |
| ------ | -------------------------- | ----------------------------------------------- |
| `GET`  | `/api/count/{domain}`      | Count of webmentions sent to a specific domain. |
| `GET`  | `/api/count/?url={target}` | Count of webmentions sent to a specific URL.    |

An example response:

```json
{
    "count": "<integer>"
}
```

### Sending webmentions

| Method | Path                       | Description                                     |
| ------ | -------------------------- | ----------------------------------------------- |
| `POST`  | `/api/send/`      | Count of webmentions sent to a specific domain. |

| Post Params | Description                                                                                                                                               | Default |
| ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| `source`    | Where webmention is coming from. The page that mentioned `target`.                                                                                        | None    |
| `target`    | Where webmention is sent to. The page that was mentioned by `source`. Target's domain should be in the allow list of receiver. (see [configuration](./configuration.md)) | None    |

Currently implemented as an alias of `/endpoint`.

Will support sending webmentions to other servers in the future. See issue [#24](https://codeberg.org/eltrac/weepinbell/issues/24).

