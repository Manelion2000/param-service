alter table bank_transaction
    add column if not exists operation_nature varchar(32);
