create table if not exists t_func_history
(
    id          integer
        constraint t_func_history_pk
            primary key autoincrement,
    func_type   text    not null,
    summary     text,
    input_text  text,
    output_text text,
    extra_data  text,
    create_time datetime
);

create index if not exists t_func_history_func_type_create_time_index
    on t_func_history (func_type, create_time desc);
