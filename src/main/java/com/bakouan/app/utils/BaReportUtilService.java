package com.bakouan.app.utils;

import com.bakouan.app.dto.EReportFormat;
import com.bakouan.app.dto.EReportSource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Cette classe permet de generer les états.
 *
 * @author :  <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2022, Yandoama Consulting All rights reserved.
 * @since : 30/06/2021 à 16:22
 */
@Service
@Data
@Slf4j
@AllArgsConstructor
public class BaReportUtilService {

    /**
     * Methode pour generer les états.
     *
     * @param fichier        Fichier d'état sous format .jrxml (obtenir a partir du context du controleur
     *                       (this.getClass().getResourceAsStream("/reports/report.jrxml");)
     * @param parametres     Constantes à passer en paramètres
     * @param jsonDataSource Source de données
     * @param format         Format d'export du rapport (PDF, EXCEL, CSV)
     * @param source
     * @return le fichier binaires du rapport
     */
    public byte[] genererRapport(final @NotNull InputStream fichier,
                                 final HashMap<String, ? super Object> parametres,
                                 final JRDataSource jsonDataSource, final @NotNull EReportFormat format,
                                 final @NotNull EReportSource source) {

        byte[] fluxFichier = null;

        JasperPrint jasperPrint = null;

        try {
            //Compile la source du fichier
            if (source.equals(EReportSource.SOURCE)) {
                JasperReport jasperReport = JasperCompileManager.compileReport(fichier);

                //Execution du rapport
                jasperPrint = JasperFillManager.fillReport(jasperReport, parametres, jsonDataSource);

            } else {
                jasperPrint = JasperFillManager.fillReport(fichier, parametres, jsonDataSource);
            }

            switch (format) {
                case PDF -> fluxFichier = imprimerPDF(jasperPrint);
                case XLSX -> fluxFichier = imprimerEXCEL(jasperPrint);
                case CSV -> fluxFichier = imprimerCSV(jasperPrint);
                default -> {
                    return null;
                }
            }

        } catch (Exception ex) {
            log.error("Erreur de generation du report", ex);
        }

        return fluxFichier;
    }

    /**
     * Methode pour generer les états en prenant le chemin d'accès du fichier en lieu
     * et place du binaire. Les fichiers passés sont supposés de la forme
     * suivante : <code>config/reports/report.jasper</code>, c'est un chemin relatif au
     * repertoire sr/main/resources C'est la version compilée qui est attendue ici
     *
     * @param chemin         Fichier d'état sous format jasper
     * @param parametres     Constantes à passer en paramètres
     * @param jsonDataSource Source de données
     * @param format         Format d'export du rapport (PDF, EXCEL, CSV)
     * @param source
     * @return le fichier binaires du rapport
     */
    public byte[] genererRapport(@NotNull final String chemin, final HashMap<String, ? super Object> parametres,
                                 final JRDataSource jsonDataSource,
                                 @NotNull final EReportFormat format, @NotNull final EReportSource source) {
        try {
            Resource r = new ClassPathResource(chemin);
            return genererRapport(r.getInputStream(), parametres, jsonDataSource, format, source);
        } catch (Exception e) {
            log.error("Erreur de generation du rapport", e);
        }
        return null;
    }

    /**
     * Cette méthode permet de generer un état sous format PDF.
     *
     * @param doc document jasper pour imprission
     * @return un flux de bytes de données
     * @throws JRException
     */
    private byte[] imprimerPDF(final JasperPrint doc) throws JRException {
        return JasperExportManager.exportReportToPdf(doc);
    }

    /**
     * Cette méthode permet de générer un état sous format Excel.
     *
     * @param doc document jasper pour imprission
     * @return un tableau de bytes
     * @throws JRException
     */
    private byte[] imprimerEXCEL(final JasperPrint doc) throws JRException {

        ByteArrayOutputStream excelReportStream = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter(); // initialize exporter
        exporter.setExporterInput(new SimpleExporterInput(doc));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(excelReportStream));
        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setOnePagePerSheet(true); // setup configuration
        configuration.setDetectCellType(true);
        exporter.setConfiguration(configuration); // set configuration
        exporter.exportReport();
        return excelReportStream.toByteArray();
    }

    /**
     * Cette méthode permet de générer un état sous format CSV.
     *
     * @param doc document jasper pour imprission
     * @return un tableau de bytes
     * @throws JRException
     */
    private byte[] imprimerCSV(final JasperPrint doc) throws JRException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        JRCsvExporter exporter = new JRCsvExporter(); // initialize exporter
        exporter.setExporterInput(new SimpleExporterInput(doc));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(byteArrayOutputStream));
        exporter.exportReport();
        return byteArrayOutputStream.toByteArray();
    }
}
