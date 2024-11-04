create table if not exists queue
(
    id         serial primary key,
    name       varchar(128) not null,
    from_id    bigint       not null,
    created_ts bigint       not null
);