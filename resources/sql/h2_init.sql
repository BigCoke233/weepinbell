create table if not exists webmention (
  id integer auto_increment primary key,
  hash text not null unique,
  wm_source text not null,
  wm_target text not null,
  wm_source_domain text not null,
  wm_target_domain text not null,
  wm_source_path text not null,
  wm_target_path text not null,
  lastmod bigint not null
);

create table if not exists microformats (
  id integer auto_increment primary key,
  wm_id integer not null,
  mf_type text not null,
  mf_data text not null, /* schema on read, store EDN string here */

  FOREIGN KEY (wm_id)
    references webmention(id)
    on delete cascade
);

create table if not exists proxied_photo (
  id integer auto_increment primary key,
  wm_id integer not null,
  url text not null,

  FOREIGN KEY (wm_id)
    references webmention(id)
    on delete cascade
);
