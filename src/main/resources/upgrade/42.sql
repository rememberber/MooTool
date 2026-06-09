create table if not exists t_translation_word
(
    id            integer
        constraint t_translation_word_pk
            primary key autoincrement,
    source_text   text    not null,
    target_text   text,
    source_lang   text,
    target_lang   text,
    remark        text,
    create_time   datetime,
    modified_time datetime
);

create index if not exists t_translation_word_modified_time_index
    on t_translation_word (modified_time desc);
