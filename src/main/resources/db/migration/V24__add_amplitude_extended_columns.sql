alter table amplitude_transaction
    add column if not exists accounting_date_raw varchar(64),
    add column if not exists value_date_raw varchar(64),
    add column if not exists piece_number varchar(128),
    add column if not exists event_number varchar(128),
    add column if not exists phone_number varchar(64);

