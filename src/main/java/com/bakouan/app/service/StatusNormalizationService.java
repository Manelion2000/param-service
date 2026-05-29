package com.bakouan.app.service;

import com.bakouan.app.enums.NormalizedBankStatus;
import com.bakouan.app.enums.NormalizedMoovStatus;
import com.bakouan.app.enums.NormalizedOrangeStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

@Service
public class StatusNormalizationService {

    public NormalizedBankStatus normalizeBankStatus(String raw) {
        if (raw == null) {
            return NormalizedBankStatus.UNKNOWN_BANK;
        }
        String v = normalizeText(raw);
        if (v.equals("alloue") || v.equals("paiement genere")) {
            return NormalizedBankStatus.SUCCESS_BANK;
        }
        if (v.equals("non alloue") || v.equals("paiement rejete") || v.equals("echec allocation")) {
            return NormalizedBankStatus.FAILED_BANK;
        }
        return NormalizedBankStatus.UNKNOWN_BANK;
    }

    public NormalizedMoovStatus normalizeMoovStatus(String raw) {
        if (raw == null) {
            return NormalizedMoovStatus.UNKNOWN_MOOV;
        }
        String v = raw.trim().toLowerCase();
        if (v.equals("completed")) {
            return NormalizedMoovStatus.SUCCESS_MOOV;
        }
        if (v.equals("cancelled")) {
            return NormalizedMoovStatus.FAILED_MOOV;
        }
        return NormalizedMoovStatus.UNKNOWN_MOOV;
    }

    public NormalizedOrangeStatus normalizeOrangeStatus(String raw) {
        if (raw == null) {
            return NormalizedOrangeStatus.UNKNOWN_ORANGE;
        }
        String v = raw.trim().toUpperCase();
        if (v.equals("TS")) {
            return NormalizedOrangeStatus.SUCCESS_ORANGE;
        }
        if (v.equals("TF")) {
            return NormalizedOrangeStatus.FAILED_ORANGE;
        }
        return NormalizedOrangeStatus.UNKNOWN_ORANGE;
    }

    private String normalizeText(String raw) {
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim().toLowerCase();
    }
}

