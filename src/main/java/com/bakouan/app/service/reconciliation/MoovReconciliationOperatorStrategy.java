package com.bakouan.app.service.reconciliation;

import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.ReconciliationReasonCode;
import com.bakouan.app.enums.ReconciliationResultType;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.repositories.FileImportRepository;
import com.bakouan.app.repositories.MoovTransactionRepository;
import com.bakouan.app.service.ReconciliationClassificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MoovReconciliationOperatorStrategy implements ReconciliationOperatorStrategy {

    private final FileImportRepository fileImportRepository;
    private final MoovTransactionRepository moovTransactionRepository;
    private final ReconciliationClassificationService classificationService;
    private final ReconciliationResultWriter resultWriter;

    @Override
    public SourceType operatorSourceType() {
        return SourceType.MOOV;
    }

    @Override
    public List<FileImport> findImports(LocalDate from, LocalDate to) {
        return fileImportRepository.findBySourceTypeAndBusinessDateBetween(SourceType.MOOV, from, to);
    }

    @Override
    public void reconcile(ReconciliationRun run, List<BankTransaction> bankRows, List<FileImport> operatorImports) {
        List<MoovTransaction> moovRows = moovTransactionRepository.findByFileImportIdIn(operatorImports.stream().map(FileImport::getId).toList());
        Map<String, List<BankTransaction>> bankByKey = bankRows.stream().collect(Collectors.groupingBy(BankTransaction::getTransactionId));
        Map<String, List<MoovTransaction>> moovByKey = moovRows.stream().collect(Collectors.groupingBy(MoovTransaction::getReceiptNo));

        Set<String> keys = new HashSet<>();
        keys.addAll(bankByKey.keySet());
        keys.addAll(moovByKey.keySet());

        for (String key : keys) {
            List<BankTransaction> banks = bankByKey.getOrDefault(key, List.of());
            List<MoovTransaction> moovs = moovByKey.getOrDefault(key, List.of());

            if (!moovs.isEmpty() && moovs.stream().allMatch(classificationService::isMoovApprovisionnement)) {
                for (MoovTransaction moov : moovs) {
                    resultWriter.save(run, key, banks.isEmpty() ? null : banks.get(0), moov, ReconciliationResultType.APPROVISIONNEMENT, resultWriter.reason(OperatorType.MOOV, ReconciliationReasonCode.APPROVISIONNEMENT));
                }
                continue;
            }
            if (banks.size() > 1) {
                for (BankTransaction bank : banks) {
                    resultWriter.save(run, key, bank, moovs.isEmpty() ? null : moovs.get(0), ReconciliationResultType.DOUBLON_BANQUE, resultWriter.reason(OperatorType.MOOV, ReconciliationReasonCode.DOUBLON_BANQUE));
                }
                continue;
            }
            if (moovs.size() > 1) {
                for (MoovTransaction moov : moovs) {
                    resultWriter.save(run, key, banks.isEmpty() ? null : banks.get(0), moov, ReconciliationResultType.DOUBLON_MOOV, resultWriter.reason(OperatorType.MOOV, ReconciliationReasonCode.DOUBLON_OPERATEUR));
                }
                continue;
            }

            BankTransaction bank = banks.isEmpty() ? null : banks.get(0);
            MoovTransaction moov = moovs.isEmpty() ? null : moovs.get(0);
            ReconciliationResultType type = classificationService.classify(bank, moov);
            resultWriter.save(run, key, bank, moov, type, resultWriter.reason(OperatorType.MOOV, reasonCode(type)));
        }
    }

    private ReconciliationReasonCode reasonCode(ReconciliationResultType type) {
        return switch (type) {
            case MATCH_OK -> ReconciliationReasonCode.MATCH_OK;
            case DEBIT_A_TORT -> ReconciliationReasonCode.DEBIT_A_TORT;
            case CREDIT_SANS_DEBIT -> ReconciliationReasonCode.CREDIT_SANS_DEBIT;
            case ECHEC_DES_DEUX_COTES -> ReconciliationReasonCode.ECHEC_DES_DEUX_COTES;
            case ABSENT_COTE_BANQUE -> ReconciliationReasonCode.ABSENT_COTE_BANQUE;
            case ABSENT_COTE_MOOV, ABSENT_COTE_ORANGE -> ReconciliationReasonCode.ABSENT_COTE_OPERATEUR;
            case OPERATEUR_NON_ABOUTI_SANS_BANQUE -> ReconciliationReasonCode.OPERATEUR_NON_ABOUTI_SANS_BANQUE;
            case APPROVISIONNEMENT -> ReconciliationReasonCode.APPROVISIONNEMENT;
            case MONTANT_DIFFERENT -> ReconciliationReasonCode.MONTANT_DIFFERENT;
            case DOUBLON_BANQUE -> ReconciliationReasonCode.DOUBLON_BANQUE;
            case DOUBLON_MOOV -> ReconciliationReasonCode.DOUBLON_OPERATEUR;
            case STATUT_INCONNU -> ReconciliationReasonCode.STATUT_INCONNU;
        };
    }
}
