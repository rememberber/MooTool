alter table t_msg_http
    add response_body text;
alter table t_msg_http
    add response_headers text;
alter table t_msg_http
    add response_cookies text;

create table if not exists t_http_request_history
(
    id            integer
        constraint t_http_request_history_pk
            primary key autoincrement,
    request_id    integer,
    method        text,
    url           text,
    params        text,
    headers       text,
    cookies       text,
    body          text,
    body_type     text,
    response_body text,
    response_headers text,
    response_cookies text,
    status        text,
    cost_time     integer,
    create_time   datetime,
    modified_time datetime
);