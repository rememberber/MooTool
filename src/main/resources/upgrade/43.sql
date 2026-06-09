create table if not exists t_translation_history
(
    id              integer
        constraint t_translation_history_pk
            primary key autoincrement,
    source_text     text    not null,
    target_text     text,
    source_lang     text,
    target_lang     text,
    translator_type text,
    create_time     datetime
);

create index if not exists t_translation_history_create_time_index
    on t_translation_history (create_time desc);
