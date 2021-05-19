create table orders_history (
  id bigint not null primary key,
  orders_id uuid not null,
  created_at timestamp not null,
  status character varying(255) not null,
  message character varying(1000)
);

CREATE SEQUENCE orders_history_seq START 1;
