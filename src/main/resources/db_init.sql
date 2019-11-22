create table if not exists t_msg_http
(
    id            integer
        constraint t_msg_http_pk
            primary key autoincrement,
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

create unique index if not exists t_msg_http_msg_name_uindex
    on t_msg_http (msg_name);

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

create table if not exists t_qr_code
(
    id            integer
        constraint t_qr_code_pk
            primary key,
    content       text,
    create_time   datetime,
    modified_time datetime
);

create table t_favorite_color_list
(
    id            integer
        constraint t_favorite_color_list_pk
            primary key autoincrement,
    title         text,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create unique index t_favorite_color_list_uindex
    on t_favorite_color_list (title);

create table t_favorite_color_item
(
    id            integer
        constraint t_favorite_color_item_pk
            primary key autoincrement,
    list_id       integer,
    name          text,
    value         text,
    sort_num      integer,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create unique index t_favorite_color_item_uindex
    on t_favorite_color_item (list_id,name);
