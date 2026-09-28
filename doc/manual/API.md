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
| `POST` | `/endpoint` (Configurable, see [configuration](./configuration.md)) |

| Post Params | Description                                                                                                                                               | Default |
| ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| `source`    | Where webmention is coming from. The page that mentioned `target`.                                                                                        | None    |
| `target`    | Where webmention is sent to. The page that was mentioned by `source`. Target's domain should be in the allow list of receiver. (see [configuration](./configuration.md)) | None    |

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

| Query Params    | Description                                                                                                                                          | Default |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| `microformats`* | Only takes value `"raw"`. By default, microformats are reduced to deterministic structure. If `"raw"`, return original unmodified microformats data. | None    |
An example response: 

```json 
{
  "webmention": {
    "source": "<source>",
    "target": "<target>",
    "id": "<32_digit_SHA_256_hash>",
    "modified": <timestamp_of_last_modified>
  },
  "microformats": {
    "author": {
      "name": "<author_name>",
      "url": "<author_url>",
      "photo": "<author_photo>"
    },
    "type": "<mention | in-reply-to | like-of | repost-of>",
    "published": "<date>",
    "content": {
        "html": "<html>",
        "text": "<text>"
    }
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

The responses are almost the same as `/api/mention/{id}` except that there are multiple webmention items wrapped inside a JSON array.

---

## Deprecated API 

### Microformats-Type-Aware routes 

Will be replaced with a query parameter of `/api/mentions/`. See [#46](https://codeberg.org/eltrac/weepinbell/issues/46).

| Method | Path                          | Description                            |
| ------ | ----------------------------- | -------------------------------------- |
| `GET`  | `/api/likes/{domain}`      | Webmentions sent to a specific domain, which contains microformats property `like-of`. |
| `GET`  | `/api/likes/?url={target}` | Webmentions sent to a specific URL, which contains microformats property `like-of`.    |
| `GET`  | `/api/reposts/{domain}`      | Webmentions sent to a specific domain, which contains microformats property `repost-of`. |
| `GET`  | `/api/reposts/?url={target}` | Webmentions sent to a specific URL, which contains microformats property `repost-of`.    |
| `GET`  | `/api/replies/{domain}`      | Webmentions sent to a specific domain., which contains microformats property `in-reply-to` |
| `GET`  | `/api/replies/?url={target}` | Webmentions sent to a specific URL, which contains microformats property `in-reply-to`.    |

