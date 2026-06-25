package com.bakouan.app.service.reconciliation;

import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationReasonCode;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.model.ReconciliationResult;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class ReconciliationResultWriter {

    private final ReconciliationResultRepository resultRepository;

    public String reason(OperatorType operator, ReconciliationReasonCode code) {
        return operator.name() + ":" + code.name();
    }

    public void save(ReconciliationRun run, String key, BankTransaction bank, MoovTransaction moov,
                     ReconciliationResultType type, String reason) {
        BigDecimal bankAmount = bank == null ? null : bank.getAmount();
        BigDecimal moovAmount = moov == null ? null : moov.getAmount();
        BigDecimal diff = (bankAmount == null || moovAmount == null) ? null : bankAmount.subtract(moovAmount);
        ReconciliationResult result = ReconciliationResult.builder()
                .run(run)
                .businessDate(resolveBusinessDate(run, bank, moov))
                .transactionKey(key)
                .resultType(type)
                .bankTransactionId(bank == null ? null : bank.getId())
                .moovTransactionId(moov == null ? null : moov.getId())
                .bankStatusRaw(bank == null ? null : bank.getAllocationStatusRaw())
                .moovStatusRaw(moov == null ? null : moov.getTransactionStatusRaw())
                .bankAmount(bankAmount)
                .moovAmount(moovAmount)
                .amountDifference(diff)
                .reason(reason)
                .createdAt(OffsetDateTime.now())
                .build();
        resultRepository.save(result);
    }

    public void save(ReconciliationRun run, String key, BankTransaction bank, OrangeTransaction orange,
                     ReconciliationResultType type, String reason) {
        BigDecimal bankAmount = bank == null ? null : bank.getAmount();
        BigDecimal orangeAmount = orange == null ? null : orange.getAmount();
        BigDecimal diff = (bankAmount == null || orangeAmount == null) ? null : bankAmount.subtract(orangeAmount);
        ReconciliationResult result = ReconciliationResult.builder()
                .run(run)
                .businessDate(resolveBusinessDate(run, bank, orange))
                .transactionKey(key)
                .resultType(type)
                .bankTransactionId(bank == null ? null : bank.getId())
                .moovTransactionId(orange == null ? null : orange.getId())
                .bankStatusRaw(bank == null ? null : bank.getAllocationStatusRaw())
                .moovStatusRaw(orange == null ? null : orange.getTransactionStatusRaw())
                .bankAmount(bankAmount)
                .moovAmount(orangeAmount)
                .amountDifference(diff)
                .reason(reason)
                .createdAt(OffsetDateTime.now())
                .build();
        resultRepository.save(result);
    }

    private LocalDate resolveBusinessDate(ReconciliationRun run, BankTransaction bank, MoovTransaction moov) {
        LocalDate bankDate = toDate(bank == null ? null : bank.getTransactionDate());
        if (bankDate != null) {
            return bankDate;
        }
        LocalDate moovDate = resolveMoovDate(moov);
        if (moovDate != null) {
            return moovDate;
        }
        return resolveFromImportsOrRun(
                run,
                bank == null ? null : bank.getFileImport(),
                moov == null ? null : moov.getFileImport()
        );
    }

    private LocalDate resolveBusinessDate(ReconciliationRun run, BankTransaction bank, OrangeTransaction orange) {
        LocalDate bankDate = toDate(bank == null ? null : bank.getTransactionDate());
        if (bankDate != null) {
            return bankDate;
        }
        LocalDate orangeDate = toDate(orange == null ? null : orange.getTransactionDateTime());
        if (orangeDate != null) {
            return orangeDate;
        }
        return resolveFromImportsOrRun(
                run,
                bank == null ? null : bank.getFileImport(),
                orange == null ? null : orange.getFileImport()
        );
    }

    private LocalDate resolveFromImportsOrRun(ReconciliationRun run, FileImport bankImport, FileImport operatorImport) {
        if (bankImport != null && bankImport.getBusinessDate() != null) {
            return bankImport.getBusinessDate();
        }
        if (operatorImport != null && operatorImport.getBusinessDate() != null) {
            return operatorImport.getBusinessDate();
        }
        return run.getBusinessDateFrom();
    }

    private LocalDate resolveMoovDate(MoovTransaction moov) {
        if (moov == null) {
            return null;
        }
        LocalDate completion = toDate(moov.getCompletionTime());
        if (completion != null) {
            return completion;
        }
        return toDate(moov.getInitiationTime());
    }

    private LocalDate toDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }
}
