package com.bakouan.app.service.parser;

import com.bakouan.app.enums.SourceType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Component
public class XlsxTransactionFileParser implements TransactionFileParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".xlsx");
    }

    @Override
    public List<Map<String, String>> parse(MultipartFile file, SourceType sourceType) {
        return XlsTransactionFileParser.readWorkbook(file, sourceType);
    }
}

