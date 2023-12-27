create table if not exists seller(
  id bigint primary key,
  user_id bigint not null unique
);
