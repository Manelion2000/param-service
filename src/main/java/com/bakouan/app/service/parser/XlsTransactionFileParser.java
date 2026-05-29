package com.bakouan.app.service.parser;

import com.bakouan.app.enums.SourceType;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class XlsTransactionFileParser implements TransactionFileParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".xls");
    }

    @Override
    public List<Map<String, String>> parse(MultipartFile file, SourceType sourceType) {
        return readWorkbook(file, sourceType);
    }

    static List<Map<String, String>> readWorkbook(MultipartFile file, SourceType sourceType) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRowIndex(sheet, formatter, sourceType);
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                throw new ParserValidationException("Le fichier Excel ne contient pas de ligne d'entete.");
            }
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(ParserSupport.normalizeHeader(formatter.formatCellValue(cell)));
            }
            ParserSupport.validateRequiredHeaders(Set.copyOf(headers), sourceType);
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> rawValues = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    rawValues.put(headers.get(c), cell == null ? "" : formatter.formatCellValue(cell));
                }
                Map<String, String> values = ParserSupport.normalizeRow(rawValues, headers);
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    values.put("_c" + c, cell == null ? "" : formatter.formatCellValue(cell));
                }
                if (!ParserSupport.isBlankRow(values)) {
                    values.put("_line_number", String.valueOf(i + 1));
                    rows.add(values);
                }
            }
            return rows;
        } catch (ParserValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserValidationException("Echec parsing Excel: " + e.getMessage(), e);
        }
    }

    private static int findHeaderRowIndex(Sheet sheet, DataFormatter formatter, SourceType sourceType) {
        int maxRow = Math.min(sheet.getLastRowNum(), 30);
        for (int r = 0; r <= maxRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            List<String> headers = new ArrayList<>();
            for (Cell cell : row) {
                headers.add(ParserSupport.normalizeHeader(formatter.formatCellValue(cell)));
            }
            try {
                ParserSupport.validateRequiredHeaders(Set.copyOf(headers), sourceType);
                return r;
            } catch (ParserValidationException ignored) {
                // Keep scanning: some input files place metadata before the actual header row.
            }
        }
        throw new ParserValidationException("Colonnes obligatoires manquantes.");
    }
}

