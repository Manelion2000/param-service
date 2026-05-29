package com.bakouan.app.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {
    Path store(MultipartFile file);
    void deleteIfExists(String filePath);
}

