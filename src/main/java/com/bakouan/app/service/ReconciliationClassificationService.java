package com.bakouan.app.service;

import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.repositories.AmplitudeTransactionRepository;
import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.NormalizedMoovStatus;
import com.bakouan.app.enums.NormalizedOrangeStatus;
import com.bakouan.app.enums.ReconciliationResultType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReconciliationClassificationService {

    private final BigDecimal tolerance;
    private final AmplitudeTransactionRepository amplitudeTransactionRepository;

    public ReconciliationClassificationService(@Value("${app.reconciliation.amount-tolerance:0.00}") BigDecimal tolerance,
                                               AmplitudeTransactionRepository amplitudeTransactionRepository) {
        this.tolerance = tolerance;
        this.amplitudeTransactionRepository = amplitudeTransactionRepository;
    }

    public ReconciliationResultType classify(BankTransaction bank, MoovTransaction moov) {
        if (isMoovApprovisionnement(moov)) {
            return ReconciliationResultType.APPROVISIONNEMENT;
        }
        if (bank == null && moov != null) {
            if (moov.getTransactionStatusNormalized() == NormalizedMoovStatus.SUCCESS_MOOV) {
                return ReconciliationResultType.ABSENT_COTE_BANQUE;
            }
            if (moov.getTransactionStatusNormalized() == NormalizedMoovStatus.FAILED_MOOV) {
                return ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE;
            }
            return ReconciliationResultType.STATUT_INCONNU;
        }
        if (bank != null && moov == null) {
            return ReconciliationResultType.ABSENT_COTE_MOOV;
        }
        if (bank == null) {
            return ReconciliationResultType.STATUT_INCONNU;
        }

        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.UNKNOWN_BANK
                || moov.getTransactionStatusNormalized() == NormalizedMoovStatus.UNKNOWN_MOOV) {
            return ReconciliationResultType.STATUT_INCONNU;
        }
        if (isGeneratedAndAccountedInAmplitude(bank)) {
            return ReconciliationResultType.MATCH_OK;
        }

        if (bank.getAmount() != null && moov.getAmount() != null) {
            BigDecimal diff = bank.getAmount().subtract(moov.getAmount()).abs();
            if (diff.compareTo(tolerance) > 0) {
                return ReconciliationResultType.MONTANT_DIFFERENT;
            }
        }

        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.SUCCESS_BANK
                && moov.getTransactionStatusNormalized() == NormalizedMoovStatus.SUCCESS_MOOV) {
            return ReconciliationResultType.MATCH_OK;
        }
        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.SUCCESS_BANK
                && moov.getTransactionStatusNormalized() == NormalizedMoovStatus.FAILED_MOOV) {
            return ReconciliationResultType.DEBIT_A_TORT;
        }
        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.FAILED_BANK
                && moov.getTransactionStatusNormalized() == NormalizedMoovStatus.SUCCESS_MOOV) {
            return ReconciliationResultType.CREDIT_SANS_DEBIT;
        }
        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.FAILED_BANK
                && moov.getTransactionStatusNormalized() == NormalizedMoovStatus.FAILED_MOOV) {
            return ReconciliationResultType.ECHEC_DES_DEUX_COTES;
        }
        return ReconciliationResultType.STATUT_INCONNU;
    }

    public boolean isMoovApprovisionnement(MoovTransaction moov) {
        if (moov == null) {
            return false;
        }
        if (moov.getTransactionStatusNormalized() != NormalizedMoovStatus.SUCCESS_MOOV) {
            return false;
        }
        String digits = moov.getMsisdn() == null ? "" : moov.getMsisdn().replaceAll("\\D", "");
        return digits.length() > 12;
    }

    public ReconciliationResultType classify(BankTransaction bank, OrangeTransaction orange) {
        if (isOrangeApprovisionnement(orange)) {
            return ReconciliationResultType.APPROVISIONNEMENT;
        }
        if (bank == null && orange != null) {
            if (orange.getTransactionStatusNormalized() == NormalizedOrangeStatus.SUCCESS_ORANGE) {
                return ReconciliationResultType.ABSENT_COTE_BANQUE;
            }
            if (orange.getTransactionStatusNormalized() == NormalizedOrangeStatus.FAILED_ORANGE) {
                return ReconciliationResultType.OPERATEUR_NON_ABOUTI_SANS_BANQUE;
            }
            return ReconciliationResultType.STATUT_INCONNU;
        }
        if (bank != null && orange == null) {
            if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.SUCCESS_BANK) {
                return ReconciliationResultType.DEBIT_A_TORT;
            }
            return ReconciliationResultType.ABSENT_COTE_ORANGE;
        }
        if (bank == null) {
            return ReconciliationResultType.STATUT_INCONNU;
        }

        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.UNKNOWN_BANK
                || orange.getTransactionStatusNormalized() == NormalizedOrangeStatus.UNKNOWN_ORANGE) {
            return ReconciliationResultType.STATUT_INCONNU;
        }
        if (isGeneratedAndAccountedInAmplitude(bank)) {
            return ReconciliationResultType.MATCH_OK;
        }

        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.SUCCESS_BANK
                && orange.getTransactionStatusNormalized() == NormalizedOrangeStatus.SUCCESS_ORANGE) {
            return ReconciliationResultType.MATCH_OK;
        }
        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.SUCCESS_BANK
                && orange.getTransactionStatusNormalized() == NormalizedOrangeStatus.FAILED_ORANGE) {
            return ReconciliationResultType.DEBIT_A_TORT;
        }
        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.FAILED_BANK
                && orange.getTransactionStatusNormalized() == NormalizedOrangeStatus.SUCCESS_ORANGE) {
            return ReconciliationResultType.CREDIT_SANS_DEBIT;
        }
        if (bank.getAllocationStatusNormalized() == NormalizedBankStatus.FAILED_BANK
                && orange.getTransactionStatusNormalized() == NormalizedOrangeStatus.FAILED_ORANGE) {
            return ReconciliationResultType.ECHEC_DES_DEUX_COTES;
        }
        return ReconciliationResultType.STATUT_INCONNU;
    }

    public boolean isOrangeApprovisionnement(OrangeTransaction orange) {
        if (orange == null) {
            return false;
        }
        if (orange.getTransactionStatusNormalized() != NormalizedOrangeStatus.SUCCESS_ORANGE) {
            return false;
        }
        String digits = orange.getSenderMobileNumber() == null ? "" : orange.getSenderMobileNumber().replaceAll("\\D", "");
        return digits.length() > 12;
    }

    private boolean isGeneratedAndAccountedInAmplitude(BankTransaction bank) {
        if (bank == null) return false;
        String raw = bank.getAllocationStatusRaw();
        if (raw == null || !raw.trim().equalsIgnoreCase("paiement généré")) {
            return false;
        }
        String ref = bank.getOperationReference();
        if (ref == null || ref.isBlank()) {
            return false;
        }
        return amplitudeTransactionRepository.existsByOperationReference(ref.trim());
    }

}

