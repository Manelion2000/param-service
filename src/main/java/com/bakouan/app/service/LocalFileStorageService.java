package com.bakouan.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root;

    public LocalFileStorageService(@Value("${app.storage.path:./datas}") String storagePath) {
        this.root = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public Path store(MultipartFile file) {
        try {
            Files.createDirectories(root);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path target = root.resolve(filename);
            Files.copy(file.getInputStream(), target);
            return target;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot store file", e);
        }
    }

    @Override
    public void deleteIfExists(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception ignored) {
            // Deletion of physical file must not block business deletion in DB.
        }
    }
}

