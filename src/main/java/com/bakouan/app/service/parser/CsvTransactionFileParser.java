package com.bakouan.app.service.parser;

import com.bakouan.app.enums.SourceType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CsvTransactionFileParser implements TransactionFileParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    @Override
    public List<Map<String, String>> parse(MultipartFile file, SourceType sourceType) {
        try {
            byte[] content = file.getBytes();
            Charset charset = StandardCharsets.UTF_8;
            String firstLine;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(content), charset))) {
                firstLine = reader.readLine();
            }

            char delimiter = detectDelimiter(firstLine, sourceType);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setIgnoreSurroundingSpaces(true)
                    .setTrim(true)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();

            try (CSVParser parser = format.parse(new InputStreamReader(new ByteArrayInputStream(content), charset))) {
                List<String> headers = parser.getHeaderNames().stream().map(ParserSupport::normalizeHeader).toList();
                Set<String> headerSet = Set.copyOf(headers);
                ParserSupport.validateRequiredHeaders(headerSet, sourceType);

                List<Map<String, String>> rows = new ArrayList<>();
                for (CSVRecord record : parser) {
                    Map<String, String> rawRow = new LinkedHashMap<>();
                    for (String header : parser.getHeaderMap().keySet()) {
                        rawRow.put(header, record.get(header));
                    }
                    Map<String, String> normalizedRow = ParserSupport.normalizeRow(rawRow, parser.getHeaderNames());
                    if (!ParserSupport.isBlankRow(normalizedRow)) {
                        rows.add(normalizedRow);
                    }
                }
                return rows;
            }
        } catch (ParserValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserValidationException("Echec parsing CSV: " + e.getMessage(), e);
        }
    }

    private char detectDelimiter(String headerLine, SourceType sourceType) {
        if (sourceType == SourceType.ORANGE) {
            return ';';
        }
        if (headerLine == null) {
            return ',';
        }
        int semicolonCount = countChar(headerLine, ';');
        int commaCount = countChar(headerLine, ',');
        return semicolonCount > commaCount ? ';' : ',';
    }

    private int countChar(String value, char ch) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
}

