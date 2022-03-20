create table if not exists t_favorite_regex_list
(
    id            integer
        constraint t_favorite_regex_list_pk
            primary key autoincrement,
    title         text,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_favorite_regex_list_uindex
    on t_favorite_regex_list (title);

create table if not exists t_favorite_regex_item
(
    id            integer
        constraint t_favorite_regex_item_pk
            primary key autoincrement,
    list_id       integer,
    name          text,
    value         text,
    sort_num      integer,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_favorite_regex_item_uindex
    on t_favorite_regex_item (list_id, name);

INSERT INTO t_favorite_regex_list (id, title, remark, create_time, modified_time)
VALUES (1, '默认收藏夹', null, '1574434141000', '1574434144000');