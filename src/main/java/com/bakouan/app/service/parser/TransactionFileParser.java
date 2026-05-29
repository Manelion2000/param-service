package com.bakouan.app.service.parser;

import com.bakouan.app.enums.SourceType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TransactionFileParser {
    boolean supports(String filename);
    List<Map<String, String>> parse(MultipartFile file, SourceType sourceType);
}

