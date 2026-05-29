package com.bakouan.app.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ParseUtils {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );

    private ParseUtils() {
    }

    public static BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value
                .replace(" ", "")
                .replace("\u00A0", "")
                .trim();

        if (normalized.contains(",") && normalized.contains(".")) {
            int lastComma = normalized.lastIndexOf(',');
            int lastDot = normalized.lastIndexOf('.');
            if (lastComma > lastDot) {
                normalized = normalized.replace(".", "").replace(',', '.');
            } else {
                normalized = normalized.replace(",", "");
            }
        } else if (normalized.contains(",")) {
            int lastComma = normalized.lastIndexOf(',');
            int decimals = normalized.length() - lastComma - 1;
            if (decimals == 3) {
                normalized = normalized.replace(",", "");
            } else {
                normalized = normalized.replace(',', '.');
            }
        } else if (normalized.contains(".")) {
            int lastDot = normalized.lastIndexOf('.');
            int decimals = normalized.length() - lastDot - 1;
            if (decimals == 3) {
                normalized = normalized.replace(".", "");
            }
        }

        return new BigDecimal(normalized);
    }

    public static BigDecimal parseAbsAmount(String value) {
        BigDecimal amount = parseAmount(value);
        return amount == null ? null : amount.abs();
    }

    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value.trim(), formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static LocalDate parseDate(String value) {
        LocalDateTime dt = parseDateTime(value);
        return dt == null ? null : dt.toLocalDate();
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public static String normalizeAccountNumber(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", "");
        cleaned = cleaned.replaceFirst("^0+", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    public static String compactIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    public static String digitsOnlyIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[^0-9]", "");
        return cleaned.isBlank() ? null : cleaned;
    }
}

