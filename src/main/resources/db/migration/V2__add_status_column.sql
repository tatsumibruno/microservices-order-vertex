alter table orders add column status character varying(255);
update orders set status = 'FINISHED';
alter table orders alter column status set not null;
