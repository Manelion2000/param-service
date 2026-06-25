update bank_transaction
set operation_nature = 'BANK_TO_WALLET'
where operation_nature is null
  and raw_payload_json ilike '%OPERATIONNATURE_=BankToWallet%';

update bank_transaction
set operation_nature = 'WALLET_TO_BANK'
where operation_nature is null
  and raw_payload_json ilike '%OPERATIONNATURE_=WalletToBank%';
