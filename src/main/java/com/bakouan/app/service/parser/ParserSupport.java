package com.bakouan.app.service.parser;

import com.bakouan.app.enums.SourceType;

import java.util.*;

final class ParserSupport {

    private static final Map<SourceType, List<List<String>>> REQUIRED_HEADER_ALIASES = Map.of(
            SourceType.BANQUE, List.of(List.of("ID transaction")),
            SourceType.MOOV, List.of(
                    List.of("Receipt No.", "Receipt No", "RECEIPT_NO", "receipt_no", "receiptno", "receipt"),
                    List.of("Transaction Status", "TRANSACTION_STATUS", "transaction_status", "STATUS", "status")
            ),
            SourceType.ORANGE, List.of(
                    List.of("OM_TRANSACTION_ID"),
                    List.of("ALIAS_BANKACCOUNTNUMBER"),
                    List.of("TRANSACTION_DATE_TIME"),
                    List.of("TRANSACTION_AMOUNT", "AMOUNT", "MONTANT"),
                    List.of("TRANSACTION_STATUS", "STATUS")
            ),
            SourceType.AMPLITUDE, List.of(
                    List.of("libelle", "Libelle", "LIBELLE")
            )
    );

    private ParserSupport() {
    }

    static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace('\u00A0', ' ').replace("\uFEFF", "");
    }

    static boolean isBlankRow(Map<String, String> row) {
        for (String value : row.values()) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static void validateRequiredHeaders(Set<String> headers, SourceType sourceType) {
        List<List<String>> requiredGroups = REQUIRED_HEADER_ALIASES.getOrDefault(sourceType, List.of());
        List<String> missing = new ArrayList<>();

        for (List<String> aliases : requiredGroups) {
            boolean found = aliases.stream().anyMatch(headers::contains);
            if (!found) {
                missing.add(String.join(" | ", aliases));
            }
        }

        if (!missing.isEmpty()) {
            throw new ParserValidationException("Colonnes obligatoires manquantes: " + String.join(", ", missing));
        }
    }

    static Map<String, String> normalizeRow(Map<String, String> rawRow, List<String> headersInOrder) {
        Map<String, String> row = new LinkedHashMap<>();
        for (String header : headersInOrder) {
            String normalizedHeader = normalizeHeader(header);
            String value = rawRow.get(header);
            if (value == null) {
                value = rawRow.get(normalizedHeader);
            }
            row.put(normalizedHeader, value == null ? "" : value.trim());
        }
        return row;
    }
}
