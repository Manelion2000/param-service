package com.bakouan.app.service;

import com.bakouan.app.dto.BaProductDto;
import com.bakouan.app.dto.EReportFormat;
import com.bakouan.app.dto.EReportSource;
import com.bakouan.app.utils.BaConstants;
import com.bakouan.app.utils.BaReportUtilService;
import com.bakouan.app.utils.BaUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BaReportService {


    private final BaReportUtilService reportGeneratorService;

    /**
     * Calls the utility to build the report and sends the report back.
     *
     * @param dto          Json dto containing the data
     * @param parameterMap if exists
     * @return ReportingResponseDto
     * @throws IOException
     * @throws JRException
     */
    private byte[] buildReport(
            final String reportTemplate,
            final EReportFormat format,
            final Object dto,
            final HashMap<String, ? super Object> parameterMap) throws IOException, JRException {

        InputStream fileInputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(reportTemplate);

        if (fileInputStream == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "jasper file : " + reportTemplate + " not found.");
        }
        // convert DTO into the JsonDatasource
        InputStream jsonFile = BaUtils.convertDtoToInputStream(dto);
        JRDataSource jsonDataSource = new JsonDataSource(jsonFile);

        return reportGeneratorService
                .genererRapport(fileInputStream, parameterMap, jsonDataSource, format, EReportSource.BINARY);
    }

    /**
     * Imprime la liste des produits.
     *
     * @return le fichier pdf en binaire
     */
    public byte[] printProduits() {
        try {
            final List<BaProductDto> datas =
                    Arrays.asList(
                            new BaProductDto("PR001", "Smartphone XYZ", "Un smartphone haut de gamme avec écran OLED de 6,5 pouces et caméra triple objectif", 799.99, "CAT-ELEC", "Électronique"),
                            new BaProductDto("PR002", "Ordinateur portable ABC", "Ordinateur portable léger avec processeur i7 et 16 Go de RAM", 1299.99, "CAT-ELEC", "Électronique"),
                            new BaProductDto("PR003", "Chaise de bureau ergonomique", "Chaise de bureau confortable avec support lombaire ajustable", 249.99, "CAT-MEUB", "Mobilier"),
                            new BaProductDto("PR004", "Casque audio sans fil", "Casque bluetooth avec réduction de bruit active", 199.99, "CAT-ELEC", "Électronique"),
                            new BaProductDto("PR005", "Livre 'Le guide du développeur Java'", "Manuel complet pour apprendre et maîtriser Java", 39.99, "CAT-LIV", "Livres"),
                            new BaProductDto("PR006", "Machine à café automatique", "Machine à café avec broyeur intégré et 15 bars de pression", 599.99, "CAT-ELECM", "Électroménager"),
                            new BaProductDto("PR007", "Sac à dos pour ordinateur portable", "Sac à dos spacieux avec compartiment rembourré pour ordinateur jusqu'à 15 pouces", 79.99, "CAT-ACC", "Accessoires"),
                            new BaProductDto("PR008", "Montre connectée Sport+", "Montre intelligente avec GPS intégré et suivi de la fréquence cardiaque", 299.99, "CAT-ELEC", "Électronique"),
                            new BaProductDto("PR009", "Enceinte bluetooth portable", "Enceinte waterproof avec 20 heures d'autonomie", 129.99, "CAT-ELEC", "Électronique"),
                            new BaProductDto("PR010", "Lampe de bureau LED", "Lampe de bureau ajustable avec différents modes d'éclairage", 49.99, "CAT-MEUB", "Mobilier")
                    );
            final HashMap<String, Object> parameters = new HashMap<>();

            parameters.put(BaConstants.REPORTS.PARAM_TITLE, "Liste des produits");

            final byte[] bytes = buildReport(BaConstants.REPORTS.REPORT_PRODUIT,
                    EReportFormat.PDF, datas, parameters);
            BaUtils.createFileFromByteArray(bytes, "./target/produit.pdf");
            return bytes;
        } catch (Exception e) {
            log.error("Erreur de generation du report", e);
            return new byte[]{};
        }
    }

}
