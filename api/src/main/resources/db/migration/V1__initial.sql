create table orders (
  id bigint not null primary key,
  code uuid not null,
  created_at timestamp not null,
  customer_name character varying(255) not null,
  customer_email character varying(255) not null,
  delivery_address character varying(1000) not null
);

CREATE UNIQUE INDEX orders_code ON orders (code);
CREATE SEQUENCE orders_seq START 1;
