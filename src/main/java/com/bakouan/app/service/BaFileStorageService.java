package com.bakouan.app.service;

import com.bakouan.app.utils.BaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Classe de gestion du stockage des fichiers dans le système de fichier.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class BaFileStorageService {

    @Value("${app.storage.path}")
    private String basePath;

    /**
     * Récupérer le contenu d'un fichier.
     *
     * @param idDoc identifiant du fichier.
     * @return un tableau de byte
     */
    public byte[] get(final String idDoc) {
        File file = new File(basePath + File.separator + idDoc);
        try {
            if (file.exists()) {
                return Files.readAllBytes(file.toPath());
            } else {
                log.debug("Fichier inexistant : {}", idDoc);
                return new byte[]{};
            }
        } catch (IOException e) {
            log.error("Erreur de chargement du fichier : " + idDoc, e);
            return new byte[]{};
        }
    }

    /**
     * Enregistrer un document.
     *
     * @param content le contenu du fichier avec les metadata
     * @return l'id généré après l'enregistrement
     */
    public String save(final byte[] content) {
        String id = BaUtils.randomUUID();
        if (content != null && content.length > 0) {
            File file = new File(basePath + File.separator + id);

            try {
                log.debug("Saving File :  {} in : {}", id, file.getAbsolutePath());

                final Path path = Path.of(basePath);
                if (!Files.exists(path)) {
                    Files.createDirectory(path);
                }

                Files.write(file.toPath(), content,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                return id;
            } catch (IOException e) {
                log.error("Erreur d'enregistrement du fichier", e);
            }
        }
        return null;
    }

    /**
     * Mettre à jour un document.
     *
     * @param idDocument identifiant de l'imgae
     * @param content    le contenu du fichier avec les metadata
     * @return l'id généré après l'enregistrement
     */
    public boolean update(final String idDocument, final byte[] content) {

        if (content != null && content.length > 0) {
            File file = new File(basePath + File.separator + idDocument);
            try {
                log.debug("Saving File :  {} in : {}", idDocument, file.getAbsolutePath());
                Files.write(file.toPath(), content,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                return true;
            } catch (IOException e) {
                log.error("Erreur de modification du fichier", e);
            }
        }
        return false;
    }

    /**
     * Supprimer un fichier.
     *
     * @param idDocument identifiant du fichier
     * @return <code>true</code>, si la suppression réussi
     */
    public boolean remove(final String idDocument) {
        log.warn("Removing file {}", idDocument);
        File file = new File(basePath + File.separator + idDocument);
        try {
            if (file.exists()) {
                Files.delete(file.toPath());
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to remove file", e);
            return false;
        }
    }
}
