alter table orders_history drop column orders_id;
alter table orders_history add column orders_id bigint not null;
alter table orders_history add constraint orders_history_fk_orders foreign key (orders_id) references orders (id);