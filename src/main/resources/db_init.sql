create table if not exists t_msg_http
(
    id            integer
        constraint t_msg_http_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    method        text,
    url           text,
    params        text,
    headers       text,
    cookies       text,
    body          text,
    body_type     text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_msg_http_msg_type_msg_name_uindex
    on t_msg_http (msg_type, msg_name);

create table if not exists t_quick_note
(
    id            integer
        constraint t_quick_note_pk
            primary key autoincrement,
    name          text,
    content       text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_quick_note_name_uindex
    on t_quick_note (name);

create table if not exists t_json_beauty
(
    id            integer
        constraint t_json_beauty_pk
            primary key autoincrement,
    name          text,
    content       text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_json_beauty_name_uindex
    on t_json_beauty (name);

create table if not exists t_host
(
    id            integer
        constraint t_host_pk
            primary key autoincrement,
    name          text,
    content       text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_host_name_uindex
    on t_host (name);