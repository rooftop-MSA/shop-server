create table if not exists seller(
  id bigint primary key,
  user_id bigint not null unique
  created_at TIMESTAMP(6) not null,
  modified_at TIMESTAMP(6) not null,
);

create table if not exists product(
  id bigint primary key,
  seller_id bigint not null,
  title text not null,
  description text not null,
  price bigint not null check (price > 0),
  quantity bigint not null check (quantity >= 0),
  version int not null,
  created_at TIMESTAMP(6) not null,
  modified_at TIMESTAMP(6) not null,
);
