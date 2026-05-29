package com.bakouan.app.service.impl;

import com.bakouan.app.enums.ImportStatus;
import com.bakouan.app.enums.OperatorType;
import com.bakouan.app.enums.SourceType;
import com.bakouan.app.dto.ImportBulkDeletionResult;
import com.bakouan.app.dto.ImportDeletionResult;
import com.bakouan.app.dto.ImportDeletionPreviewResult;
import com.bakouan.app.dto.ImportFullDeletionResult;
import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.FileImport;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.model.AmplitudeTransaction;
import com.bakouan.app.model.ReconciliationRun;
import com.bakouan.app.repositories.AmplitudeTransactionRepository;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.FileImportRepository;
import com.bakouan.app.repositories.MoovTransactionRepository;
import com.bakouan.app.repositories.OrangeTransactionRepository;
import com.bakouan.app.repositories.ReconciliationResultRepository;
import com.bakouan.app.repositories.ReconciliationRunRepository;
import com.bakouan.app.service.FileImportService;
import com.bakouan.app.service.ReconciliationException;
import com.bakouan.app.service.FileStorageService;
import com.bakouan.app.service.StatusNormalizationService;
import com.bakouan.app.service.parser.ParserValidationException;
import com.bakouan.app.service.parser.TransactionFileParser;
import com.bakouan.app.utils.ParseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FileImportServiceImpl implements FileImportService {
    private static final Pattern SENDER_MOBILE_PATTERN = Pattern.compile("(?i)(?:^|[,;\\s])SENDER_MOBILE_NUMBER\\s*=\\s*([^,;\\s}\\]]+)");
    private static final Pattern BANK_LAST_NAME_PATTERN = Pattern.compile("(?i)(?:^|[,;\\s])Nom\\s*=\\s*([^,;}\\]]+)");
    private static final Pattern BANK_FIRST_NAME_PATTERN = Pattern.compile("(?i)(?:^|[,;\\s])(?:Pr\\S*nom|Prenom)\\s*=\\s*([^,;}\\]]+)");
    private static final Pattern BANK_MSISDN_PATTERN = Pattern.compile("(?i)(?:^|[,;\\s])MSISDN\\s*=\\s*([^,;\\s}\\]]+)");
    private static final Pattern BANK_OPERATION_REFERENCE_PATTERN = Pattern.compile("(?i)(?:^|[,;\\s])(?:R\\S*f\\S*rence\\s*op\\S*ration|Reference\\s*operation)\\s*=\\s*([^,;\\s}\\]]+)");

    private final List<TransactionFileParser> parsers;
    private final FileStorageService fileStorageService;
    private final FileImportRepository fileImportRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final MoovTransactionRepository moovTransactionRepository;
    private final OrangeTransactionRepository orangeTransactionRepository;
    private final AmplitudeTransactionRepository amplitudeTransactionRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final StatusNormalizationService statusNormalizationService;

    @Override
    @Transactional
    public FileImport importFile(SourceType sourceType, OperatorType operatorScope, LocalDate businessDate, MultipartFile file) {
        if (sourceType == SourceType.BANQUE && operatorScope == null) {
            throw new IllegalArgumentException("operator est obligatoire pour un import BANQUE (MOOV ou ORANGE)");
        }
        String checksum = checksumSha256(file);
        fileImportRepository.findDuplicateByScopeAndChecksum(sourceType, sourceType == SourceType.BANQUE ? operatorScope : null, businessDate, checksum)
                .ifPresent(existing -> {
                    int existingTxCount = countTransactionsForImport(sourceType, existing.getId());
                    if (existingTxCount <= 0) {
                        // Orphan import metadata (transactions deleted manually): cleanup and allow re-upload.
                        fileImportRepository.delete(existing);
                        fileImportRepository.flush();
                        fileStorageService.deleteIfExists(existing.getFilePath());
                        return;
                    }
                    throw new ReconciliationException(
                            "Ce fichier est deja importe pour " + sourceType + " / " + businessDate
                                    + (existing.getOperatorScope() != null ? " / " + existing.getOperatorScope() : "")
                                    + " (importId=" + existing.getId() + ")"
                    );
                });

        TransactionFileParser parser = parsers.stream()
                .filter(p -> p.supports(file.getOriginalFilename()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file extension"));

        Path storedPath = fileStorageService.store(file);
        FileImport fileImport = FileImport.builder()
                .sourceType(sourceType)
                .operatorScope(sourceType == SourceType.BANQUE ? operatorScope : null)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedPath.getFileName().toString())
                .contentType(file.getContentType())
                .checksum(checksum)
                .businessDate(businessDate)
                .importedAt(OffsetDateTime.now())
                .importStatus(ImportStatus.PENDING)
                .filePath(storedPath.toString())
                .build();
        fileImport = fileImportRepository.save(fileImport);

        int valid = 0;
        int invalid = 0;
        List<String> importWarnings = new ArrayList<>();
        List<Map<String, String>> rows;
        try {
            rows = parser.parse(file, sourceType);
        } catch (ParserValidationException e) {
            fileImport.setImportStatus(ImportStatus.FAILED);
            fileImport.setErrorMessage(e.getMessage());
            fileImport.setTotalRows(0);
            fileImport.setValidRows(0);
            fileImport.setInvalidRows(0);
            return fileImportRepository.save(fileImport);
        }

        Set<String> existingBankIds = sourceType == SourceType.BANQUE
                ? bankTransactionRepository.findExistingTransactionIds(extractBankTransactionIds(rows))
                : Set.of();
        Set<String> existingMoovReceipts = sourceType == SourceType.MOOV
                ? moovTransactionRepository.findExistingReceiptNos(extractMoovReceiptNos(rows))
                : Set.of();
        Set<String> existingOrangeOmTransactionIds = sourceType == SourceType.ORANGE
                ? orangeTransactionRepository.findExistingOmTransactionIds(extractOrangeOmTransactionIds(rows))
                : Set.of();
        Set<String> alreadyProcessedBankIds = new HashSet<>();
        Set<String> alreadyProcessedMoovReceipts = new HashSet<>();
        Set<String> alreadyProcessedOrangeOmTransactionIds = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                if (sourceType == SourceType.BANQUE) {
                    String transactionId = normalizeKey(ParseUtils.firstNonBlank(row.get("ID transaction"), row.get("Id transaction")));
                    if (transactionId == null) {
                        invalid++;
                        continue;
                    }
                    if (alreadyProcessedBankIds.contains(transactionId) || existingBankIds.contains(transactionId)) {
                        invalid++;
                        continue;
                    }
                    String bankStatus = ParseUtils.firstNonBlank(
                            row.get("Statut Allocation"),
                            row.get("Statut allocation"),
                            row.get("Status"),
                            row.get("ALLOCATIONSTATUS_")
                    );
                    bankStatus = normalizeAllocationStatusLabel(bankStatus);
                    BankTransaction bank = BankTransaction.builder()
                            .fileImport(fileImport)
                            .transactionId(ParseUtils.firstNonBlank(transactionId, normalizeKey(row.get("TRANSACTIONID_"))))
                            .allocationStatusRaw(bankStatus)
                            .allocationStatusNormalized(statusNormalizationService.normalizeBankStatus(bankStatus))
                            .fullName(extractBankFullName(row))
                            .accountNumber(extractBankAccountNumber(row))
                            .phoneNumber(extractBankPhoneNumber(row))
                            .operationReference(extractBankOperationReference(row))
                            .amount(ParseUtils.parseAbsAmount(ParseUtils.firstNonBlank(row.get("Montant"), row.get("Montant nominal"), row.get("AMOUNT_"))))
                            .transactionDate(parseBankTransactionDate(row))
                            .rawPayloadJson(row.toString())
                            .lineNumber(parseLineNumber(row, i + 2))
                            .build();
                    bankTransactionRepository.save(bank);
                    alreadyProcessedBankIds.add(transactionId);
                } else if (sourceType == SourceType.MOOV) {
                    String receiptNo = validateMoovReceiptNo(extractMoovReceiptNo(row));
                    if (receiptNo == null) {
                        invalid++;
                        if (importWarnings.size() < 50) {
                            importWarnings.add("Ligne " + parseLineNumber(row, i + 2) + ": Receipt No invalide (vide ou longueur < 11)");
                        }
                        continue;
                    }
                    if (alreadyProcessedMoovReceipts.contains(receiptNo) || existingMoovReceipts.contains(receiptNo)) {
                        invalid++;
                        if (importWarnings.size() < 50) {
                            String source = alreadyProcessedMoovReceipts.contains(receiptNo) ? "fichier" : "base";
                            importWarnings.add("Ligne " + parseLineNumber(row, i + 2) + ": Receipt No duplique (" + source + ") -> " + receiptNo);
                        }
                        continue;
                    }
                    String moovStatusRaw = extractMoovStatus(row);
                    MoovTransaction moov = MoovTransaction.builder()
                            .fileImport(fileImport)
                            .receiptNo(receiptNo)
                            .transactionStatusRaw(moovStatusRaw)
                            .transactionStatusNormalized(statusNormalizationService.normalizeMoovStatus(moovStatusRaw))
                            .transactionType(normalizeTransactionType(ParseUtils.firstNonBlank(
                                    row.get("Transaction Type"),
                                    row.get("Transfer Type"),
                                    row.get("Type"),
                                    row.get("transaction_type"),
                                    row.get("transfer_type")
                            )))
                            .msisdn(ParseUtils.firstNonBlank(row.get("Initiator MSISDN"), row.get("initiator_msisdn"), row.get("msisdn"), row.get("MSISDN")))
                            .amount(ParseUtils.parseAbsAmount(ParseUtils.firstNonBlank(row.get("Withdrawn"), row.get("Amount"), row.get("amount"), row.get("MONTANT"))))
                            .initiationTime(ParseUtils.parseDateTime(ParseUtils.firstNonBlank(row.get("Initiation Time"), row.get("initiation_time"), row.get("created_at"), row.get("creation_date"))))
                            .completionTime(ParseUtils.parseDateTime(ParseUtils.firstNonBlank(row.get("Completion Time"), row.get("completion_time"), row.get("updated_at"), row.get("transaction_date"))))
                            .rawPayloadJson(row.toString())
                            .lineNumber(parseLineNumber(row, i + 2))
                            .build();
                    moovTransactionRepository.save(moov);
                    alreadyProcessedMoovReceipts.add(receiptNo);
                } else if (sourceType == SourceType.ORANGE) {
                    String omTransactionId = normalizeKey(ParseUtils.firstNonBlank(row.get("OM_TRANSACTION_ID")));
                    if (omTransactionId == null) {
                        invalid++;
                        continue;
                    }
                    if (alreadyProcessedOrangeOmTransactionIds.contains(omTransactionId) || existingOrangeOmTransactionIds.contains(omTransactionId)) {
                        invalid++;
                        continue;
                    }

                    String normalizedAccount = ParseUtils.normalizeAccountNumber(ParseUtils.firstNonBlank(row.get("ALIAS_BANKACCOUNTNUMBER")));
                    if (normalizedAccount == null) {
                        invalid++;
                        continue;
                    }
                    String orangeStatusRaw = ParseUtils.firstNonBlank(row.get("TRANSACTION_STATUS"), row.get("STATUS"));
                    java.time.LocalDateTime transactionDateTime = ParseUtils.parseDateTime(ParseUtils.firstNonBlank(row.get("TRANSACTION_DATE_TIME")));
                    if (transactionDateTime == null) {
                        invalid++;
                        continue;
                    }
                    OrangeTransaction orange = OrangeTransaction.builder()
                            .fileImport(fileImport)
                            .omTransactionId(omTransactionId)
                            .aliasBankAccountNumber(normalizedAccount)
                            .senderMobileNumber(extractSenderMobileNumber(row))
                            .transactionStatusRaw(orangeStatusRaw)
                            .transactionStatusNormalized(statusNormalizationService.normalizeOrangeStatus(orangeStatusRaw))
                            .amount(ParseUtils.parseAbsAmount(ParseUtils.firstNonBlank(
                                    row.get("TRANSACTION_AMOUNT"),
                                    row.get("AMOUNT"),
                                    row.get("MONTANT")
                            )))
                            .transactionDateTime(transactionDateTime)
                            .rawPayloadJson(row.toString())
                            .lineNumber(parseLineNumber(row, i + 2))
                            .build();
                    orangeTransactionRepository.save(orange);
                    alreadyProcessedOrangeOmTransactionIds.add(omTransactionId);
                } else {
                    String accountingDateRaw = ParseUtils.firstNonBlank(row.get("_c0"), row.get("Date compta"));
                    String valueDateRaw = ParseUtils.firstNonBlank(row.get("_c1"), row.get("Date valeur"));
                    String pieceNumber = ParseUtils.firstNonBlank(row.get("_c3"), row.get("No piece"));
                    String eventNumber = ParseUtils.firstNonBlank(row.get("_c4"), row.get("No eve"));
                    String libelle = ParseUtils.firstNonBlank(row.get("_c6"), row.get("libelle"), row.get("Libelle"), row.get("LIBELLE"));
                    String operationRef = normalizeAmplitudeReference(ParseUtils.firstNonBlank(row.get("_c7")));
                    String phoneNumber = normalizeAmplitudePhone(ParseUtils.firstNonBlank(row.get("_c8")));
                    java.math.BigDecimal amount = ParseUtils.parseAbsAmount(ParseUtils.firstNonBlank(row.get("_c10"), row.get("Credit")));
                    boolean transactionLikeRow = amount != null
                            || (pieceNumber != null && !pieceNumber.isBlank())
                            || (eventNumber != null && !eventNumber.isBlank())
                            || (accountingDateRaw != null && !accountingDateRaw.isBlank())
                            || (valueDateRaw != null && !valueDateRaw.isBlank());
                    // Ignore report/continuation lines that are not actual transactions.
                    if (!transactionLikeRow) {
                        continue;
                    }
                    if (phoneNumber == null || phoneNumber.isBlank()) {
                        invalid++;
                        continue;
                    }
                    AmplitudeTransaction amplitude = AmplitudeTransaction.builder()
                            .fileImport(fileImport)
                            .accountingDateRaw(accountingDateRaw)
                            .valueDateRaw(valueDateRaw)
                            .pieceNumber(pieceNumber)
                            .eventNumber(eventNumber)
                            .libelle(libelle)
                            .operationReference(operationRef != null ? operationRef : extractAmplitudeOperationReference(row))
                            .phoneNumber(phoneNumber)
                            .accountNumber(ParseUtils.digitsOnlyIdentifier(ParseUtils.firstNonBlank(row.get("Compte"), row.get("Numero de compte"), row.get("Numéro de compte"))))
                            .direction(ParseUtils.firstNonBlank(row.get("Sens"), row.get("Type"), row.get("Direction")))
                            .amount(amount)
                            .operationDate(ParseUtils.parseDateTime(ParseUtils.firstNonBlank(row.get("Date operation"), row.get("Date"), row.get("Date transaction"))))
                            .rawPayloadJson(row.toString())
                            .lineNumber(parseLineNumber(row, i + 2))
                            .build();
                    amplitudeTransactionRepository.save(amplitude);
                }
                valid++;
            } catch (Exception e) {
                invalid++;
            }
        }

        fileImport.setTotalRows(rows.size());
        fileImport.setValidRows(valid);
        fileImport.setInvalidRows(invalid);
        String warningsMessage = importWarnings.isEmpty() ? null : String.join(" | ", importWarnings);
        if (valid == 0 && rows.size() > 0) {
            fileImport.setImportStatus(ImportStatus.FAILED);
            fileImport.setErrorMessage(warningsMessage != null
                    ? "Aucune ligne valide importee. " + warningsMessage
                    : "Aucune ligne valide importee.");
            return fileImportRepository.save(fileImport);
        }
        if (warningsMessage != null) {
            fileImport.setErrorMessage(warningsMessage);
        }
        fileImport.setImportStatus(invalid > 0 ? ImportStatus.PARTIAL_SUCCESS : ImportStatus.SUCCESS);
        return fileImportRepository.save(fileImport);
    }

    @Override
    public Page<FileImport> list(Pageable pageable) {
        return fileImportRepository.findAll(pageable);
    }

    @Override
    public FileImport get(Long id) {
        return fileImportRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    public ImportDeletionResult deleteLatestImport(SourceType sourceType) {
        FileImport latestImport = fileImportRepository.findTopBySourceTypeOrderByImportedAtDescIdDesc(sourceType)
                .orElseThrow(() -> new ReconciliationException("Aucun import trouve pour la source " + sourceType));

        Long importId = latestImport.getId();
        int deletedTransactions = switch (sourceType) {
            case BANQUE -> bankTransactionRepository.countByFileImportId(importId);
            case MOOV -> moovTransactionRepository.countByFileImportId(importId);
            case ORANGE -> orangeTransactionRepository.countByFileImportId(importId);
            case AMPLITUDE -> amplitudeTransactionRepository.countByFileImportId(importId);
        };

        List<ReconciliationRun> impactedRuns = reconciliationRunRepository.findAll().stream()
                .filter(run -> runContainsImportId(run, sourceType, importId))
                .toList();
        int deletedResults = 0;
        for (ReconciliationRun run : impactedRuns) {
            deletedResults += reconciliationResultRepository.countByRunId(run.getId());
            reconciliationResultRepository.deleteByRunId(run.getId());
        }
        if (!impactedRuns.isEmpty()) {
            reconciliationRunRepository.deleteAll(impactedRuns);
        }

        switch (sourceType) {
            case BANQUE -> bankTransactionRepository.deleteByFileImportId(importId);
            case MOOV -> moovTransactionRepository.deleteByFileImportId(importId);
            case ORANGE -> orangeTransactionRepository.deleteByFileImportId(importId);
            case AMPLITUDE -> amplitudeTransactionRepository.deleteByFileImportId(importId);
        }
        fileImportRepository.delete(latestImport);
        fileStorageService.deleteIfExists(latestImport.getFilePath());

        return new ImportDeletionResult(
                sourceType,
                importId,
                latestImport.getOriginalFilename(),
                deletedTransactions,
                deletedResults,
                impactedRuns.size()
        );
    }

    @Override
    @Transactional
    public ImportBulkDeletionResult deleteImportsBySourceAndBusinessDate(SourceType sourceType, OperatorType operatorScope, LocalDate businessDate) {
        List<FileImport> imports = resolveImportsBySourceAndBusinessDate(sourceType, operatorScope, businessDate);
        DeletionStats stats = deleteImports(sourceType, imports);
        return new ImportBulkDeletionResult(
                sourceType,
                businessDate,
                stats.deletedImports(),
                stats.deletedTransactions(),
                stats.deletedResults(),
                stats.deletedRuns()
        );
    }

    @Override
    @Transactional
    public ImportFullDeletionResult deleteAllImportsBySource(SourceType sourceType, OperatorType operatorScope) {
        if (sourceType == SourceType.BANQUE && operatorScope == null) {
            throw new IllegalArgumentException("operator est obligatoire pour supprimer tous les imports BANQUE (MOOV ou ORANGE)");
        }
        List<FileImport> imports = sourceType == SourceType.BANQUE
                ? new ArrayList<>(fileImportRepository.findBySourceTypeAndOperatorScope(sourceType, operatorScope))
                : new ArrayList<>(fileImportRepository.findBySourceType(sourceType));
        DeletionStats stats = deleteImports(sourceType, imports);
        return new ImportFullDeletionResult(
                sourceType,
                sourceType == SourceType.BANQUE ? operatorScope : null,
                stats.deletedImports(),
                stats.deletedTransactions(),
                stats.deletedResults(),
                stats.deletedRuns()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ImportDeletionPreviewResult previewDeletionBySourceAndBusinessDate(SourceType sourceType, OperatorType operatorScope, LocalDate businessDate) {
        List<FileImport> imports = resolveImportsBySourceAndBusinessDate(sourceType, operatorScope, businessDate);
        if (imports.isEmpty()) {
            return new ImportDeletionPreviewResult(sourceType, businessDate, 0, 0, 0, 0);
        }
        Set<Long> importIds = imports.stream().map(FileImport::getId).collect(java.util.stream.Collectors.toSet());
        int candidateTransactions = switch (sourceType) {
            case BANQUE -> imports.stream().mapToInt(i -> bankTransactionRepository.countByFileImportId(i.getId())).sum();
            case MOOV -> imports.stream().mapToInt(i -> moovTransactionRepository.countByFileImportId(i.getId())).sum();
            case ORANGE -> imports.stream().mapToInt(i -> orangeTransactionRepository.countByFileImportId(i.getId())).sum();
            case AMPLITUDE -> imports.stream().mapToInt(i -> amplitudeTransactionRepository.countByFileImportId(i.getId())).sum();
        };
        List<ReconciliationRun> impactedRuns = new ArrayList<>();
        for (ReconciliationRun run : reconciliationRunRepository.findAll()) {
            if (containsAnyImportId(run, sourceType, importIds)) {
                impactedRuns.add(run);
            }
        }
        int impactedResults = impactedRuns.stream().mapToInt(run -> reconciliationResultRepository.countByRunId(run.getId())).sum();
        return new ImportDeletionPreviewResult(
                sourceType,
                businessDate,
                imports.size(),
                candidateTransactions,
                impactedResults,
                impactedRuns.size()
        );
    }

    private Integer parseLineNumber(Map<String, String> row, int fallback) {
        String raw = row.get("_line_number");
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String normalizeTransactionType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().replace('-', '_').replace(' ', '_').toUpperCase();
    }

    private String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private String validateMoovReceiptNo(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() < 10) {
            return null;
        }
        return value;
    }

    private String extractMoovReceiptNo(Map<String, String> row) {
        String direct = ParseUtils.firstNonBlank(
                row.get("Receipt No."),
                row.get("Receipt No"),
                row.get("RECEIPT_NO"),
                row.get("receipt no"),
                row.get("receipt no."),
                row.get("receipt_no"),
                row.get("receiptno"),
                row.get("receipt")
        );
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        for (Map.Entry<String, String> e : row.entrySet()) {
            String key = e.getKey();
            if (key == null) continue;
            String k = key.trim().toLowerCase();
            if (k.contains("receipt") && k.contains("no")) {
                String v = e.getValue();
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return null;
    }

    private String extractMoovStatus(Map<String, String> row) {
        String status = ParseUtils.firstNonBlank(
                row.get("Transaction Status"),
                row.get("TRANSACTION_STATUS"),
                row.get("transaction_status"),
                row.get("STATUS"),
                row.get("status")
        );
        if (status != null && !status.isBlank()) {
            return status;
        }
        for (Map.Entry<String, String> e : row.entrySet()) {
            String key = e.getKey();
            if (key == null) continue;
            String k = key.trim().toLowerCase();
            if (k.contains("status")) {
                String v = e.getValue();
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private String buildFullName(String lastName, String firstName) {
        if ((lastName == null || lastName.isBlank()) && (firstName == null || firstName.isBlank())) {
            return null;
        }
        if (lastName == null || lastName.isBlank()) {
            return firstName.trim();
        }
        if (firstName == null || firstName.isBlank()) {
            return lastName.trim();
        }
        return (lastName.trim() + " " + firstName.trim()).trim();
    }

    private String extractSenderMobileNumber(Map<String, String> row) {
        String direct = ParseUtils.firstNonBlank(
                row.get("SENDER_MOBILE_NUMBER"),
                row.get("Sender Mobile Number"),
                row.get("sender_mobile_number")
        );
        if (direct != null) {
            return ParseUtils.compactIdentifier(direct);
        }

        String payload = ParseUtils.firstNonBlank(
                row.get("NEW_PAYLOAD_JSON"),
                row.get("new_payload_json"),
                row.get("PAYLOAD_JSON"),
                row.get("payload_json")
        );
        if (payload == null) {
            return null;
        }
        Matcher matcher = SENDER_MOBILE_PATTERN.matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return ParseUtils.compactIdentifier(matcher.group(1));
    }

    private String extractBankFullName(Map<String, String> row) {
        String lastName = ParseUtils.firstNonBlank(
                row.get("Nom"),
                row.get("NOM")
        );
        String firstName = ParseUtils.firstNonBlank(
                row.get("Prénom(s)"),
                row.get("Prenom(s)"),
                row.get("PRENOM(S)"),
                row.get("Prénom"),
                row.get("PrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©nom"),
                row.get("PrÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©nom"),
                row.get("Prenom"),
                row.get("PRENOM")
        );
        if (lastName != null || firstName != null) {
            return buildFullName(lastName, firstName);
        }

        String payload = ParseUtils.firstNonBlank(
                row.get("RAW_PAYLOAD_JSON"),
                row.get("raw_payload_json"),
                row.get("JSON_PAYLOAD_JSON"),
                row.get("json_payload_json")
        );
        if (payload == null) {
            payload = row.toString();
        }

        String payloadLastName = extractPayloadField(payload, BANK_LAST_NAME_PATTERN);
        String payloadFirstName = extractPayloadField(payload, BANK_FIRST_NAME_PATTERN);
        return buildFullName(payloadLastName, payloadFirstName);
    }


    private String extractBankAccountNumber(Map<String, String> row) {
        String direct = ParseUtils.firstNonBlank(
                row.get("Numéro de compte"),
                row.get("NumÃ©ro de compte"),
                row.get("Numero de compte"),
                row.get("Compte"),
                row.get("ACCOUNTNUMBER_")
        );
        if (direct != null) {
            return ParseUtils.digitsOnlyIdentifier(direct);
        }

        String payload = ParseUtils.firstNonBlank(
                row.get("RAW_PAYLOAD_JSON"),
                row.get("raw_payload_json"),
                row.get("JSON_PAYLOAD_JSON"),
                row.get("json_payload_json")
        );
        if (payload == null) {
            payload = row.toString();
        }

        String extracted = ParseUtils.firstNonBlank(
                extractPayloadField(payload, Pattern.compile("(?i)(?:^|[,;\\s])Num\\S*ro de compte\\s*=\\s*([^,;}\\]]+)")),
                extractPayloadField(payload, Pattern.compile("(?i)(?:^|[,;\\s])Numero de compte\\s*=\\s*([^,;}\\]]+)")),
                extractPayloadField(payload, Pattern.compile("(?i)(?:^|[,;\\s])Compte\\s*=\\s*([^,;}\\]]+)"))
        );
        return ParseUtils.digitsOnlyIdentifier(extracted);
    }

    private String extractBankPhoneNumber(Map<String, String> row) {
        String direct = ParseUtils.firstNonBlank(
                row.get("MSISDN"),
                row.get("MSIDN_"),
                row.get("Telephone"),
                row.get("TÃ©lÃ©phone"),
                row.get("TÃƒÂ©lÃƒÂ©phone"),
                row.get("Numero Telephone"),
                row.get("Numero de telephone")
        );
        if (direct != null) {
            return ParseUtils.compactIdentifier(direct);
        }

        String payload = ParseUtils.firstNonBlank(
                row.get("RAW_PAYLOAD_JSON"),
                row.get("raw_payload_json"),
                row.get("JSON_PAYLOAD_JSON"),
                row.get("json_payload_json")
        );
        if (payload == null) {
            payload = row.toString();
        }
        String extracted = extractPayloadField(payload, BANK_MSISDN_PATTERN);
        return ParseUtils.compactIdentifier(extracted);
    }

    private String extractBankOperationReference(Map<String, String> row) {
        String direct = ParseUtils.firstNonBlank(
                row.get("RÃ©fÃ©rence opÃ©ration"),
                row.get("RÃƒÂ©fÃƒÂ©rence opÃƒÂ©ration"),
                row.get("Reference operation"),
                row.get("Reference OpÃ©ration"),
                row.get("Reference Operation"),
                row.get("OPERATIONREFERENCE_"),
                row.get("Référence opération")
        );
        if (direct == null) {
            // Some Carthago Excel exports have encoding-damaged headers for column B.
            // Fallback to positional extraction: "_c1" is "Référence opération" in those files.
            direct = ParseUtils.firstNonBlank(row.get("_c1"), row.get("B"));
        }
        if (direct == null) {
            // Last fallback: fuzzy header lookup containing both "ref" and "op".
            for (Map.Entry<String, String> e : row.entrySet()) {
                String key = e.getKey();
                if (key == null) continue;
                String k = key.toLowerCase();
                if (k.contains("ref") && k.contains("op")) {
                    direct = e.getValue();
                    if (direct != null && !direct.isBlank()) break;
                }
            }
        }
        String source = direct;
        if (source == null) {
            String payload = ParseUtils.firstNonBlank(
                    row.get("RAW_PAYLOAD_JSON"),
                    row.get("raw_payload_json"),
                    row.get("JSON_PAYLOAD_JSON"),
                    row.get("json_payload_json")
            );
            if (payload == null) {
                payload = row.toString();
            }
            source = extractPayloadField(payload, BANK_OPERATION_REFERENCE_PATTERN);
        }
        if (source == null) {
            return null;
        }
        String digits = source.replaceAll("\\D", "");
        if (digits.length() < 10) {
            return null;
        }
        return digits;
    }
    private String extractPayloadField(String payload, Pattern pattern) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return ParseUtils.firstNonBlank(matcher.group(1));
    }

    private Set<String> extractBankTransactionIds(List<Map<String, String>> rows) {
        Set<String> ids = new HashSet<>();
        for (Map<String, String> row : rows) {
            String id = normalizeKey(ParseUtils.firstNonBlank(row.get("ID transaction"), row.get("Id transaction")));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private Set<String> extractMoovReceiptNos(List<Map<String, String>> rows) {
        Set<String> receiptNos = new HashSet<>();
        for (Map<String, String> row : rows) {
            String receiptNo = validateMoovReceiptNo(extractMoovReceiptNo(row));
            if (receiptNo != null) {
                receiptNos.add(receiptNo);
            }
        }
        return receiptNos;
    }

    private Set<String> extractOrangeOmTransactionIds(List<Map<String, String>> rows) {
        Set<String> omTransactionIds = new HashSet<>();
        for (Map<String, String> row : rows) {
            String omTransactionId = normalizeKey(ParseUtils.firstNonBlank(row.get("OM_TRANSACTION_ID")));
            if (omTransactionId != null) {
                omTransactionIds.add(omTransactionId);
            }
        }
        return omTransactionIds;
    }

    private boolean runContainsImportId(ReconciliationRun run, SourceType sourceType, Long importId) {
        String csvIds = switch (sourceType) {
            case BANQUE -> run.getBankImportIds();
            case MOOV -> run.getMoovImportIds();
            case ORANGE -> run.getOrangeImportIds();
            case AMPLITUDE -> null;
        };
        return parseCsvIds(csvIds).contains(importId);
    }

    private boolean containsAnyImportId(ReconciliationRun run, SourceType sourceType, Set<Long> importIds) {
        if (importIds.isEmpty()) {
            return false;
        }
        String csvIds = switch (sourceType) {
            case BANQUE -> run.getBankImportIds();
            case MOOV -> run.getMoovImportIds();
            case ORANGE -> run.getOrangeImportIds();
            case AMPLITUDE -> null;
        };
        Set<Long> runIds = parseCsvIds(csvIds);
        for (Long id : importIds) {
            if (runIds.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private List<FileImport> resolveImportsBySourceAndBusinessDate(SourceType sourceType, OperatorType operatorScope, LocalDate businessDate) {
        if (sourceType == SourceType.BANQUE && operatorScope == null) {
            throw new IllegalArgumentException("operator est obligatoire pour nettoyer les imports BANQUE (MOOV ou ORANGE)");
        }
        List<FileImport> imports = new ArrayList<>(sourceType == SourceType.BANQUE
                ? fileImportRepository.findBySourceTypeAndOperatorScopeAndBusinessDateBetween(sourceType, operatorScope, businessDate, businessDate)
                : fileImportRepository.findBySourceTypeAndBusinessDate(sourceType, businessDate));
        if (!imports.isEmpty()) {
            return imports;
        }
        LocalDateTime from = businessDate.atStartOfDay();
        LocalDateTime to = businessDate.plusDays(1).atStartOfDay();
        Set<Long> importIdsFromTxDate = switch (sourceType) {
            case BANQUE -> bankTransactionRepository.findImportIdsByTransactionDateRange(from, to);
            case MOOV -> moovTransactionRepository.findImportIdsByCompletionTimeRange(from, to);
            case ORANGE -> orangeTransactionRepository.findImportIdsByTransactionDateTimeRange(from, to);
            case AMPLITUDE -> java.util.Set.of();
        };
        if (importIdsFromTxDate.isEmpty()) {
            return imports;
        }
        imports.addAll(fileImportRepository.findAllById(importIdsFromTxDate).stream()
                .filter(fileImport -> fileImport.getSourceType() == sourceType)
                .filter(fileImport -> sourceType != SourceType.BANQUE || fileImport.getOperatorScope() == operatorScope)
                .toList());
        return imports;
    }

    private Set<Long> parseCsvIds(String csvIds) {
        if (csvIds == null || csvIds.isBlank()) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        for (String token : csvIds.split(",")) {
            try {
                ids.add(Long.parseLong(token.trim()));
            } catch (NumberFormatException ignored) {
                // Skip malformed token and continue cleanup with valid ids.
            }
        }
        return ids;
    }

    private String checksumSha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de calculer le checksum du fichier", e);
        }
    }

    private int countTransactionsForImport(SourceType sourceType, Long importId) {
        return switch (sourceType) {
            case BANQUE -> bankTransactionRepository.countByFileImportId(importId);
            case MOOV -> moovTransactionRepository.countByFileImportId(importId);
            case ORANGE -> orangeTransactionRepository.countByFileImportId(importId);
            case AMPLITUDE -> amplitudeTransactionRepository.countByFileImportId(importId);
        };
    }

    private DeletionStats deleteImports(SourceType sourceType, List<FileImport> imports) {
        if (imports.isEmpty()) {
            return new DeletionStats(0, 0, 0, 0);
        }
        Set<Long> importIds = imports.stream().map(FileImport::getId).collect(java.util.stream.Collectors.toSet());
        int deletedTransactions = switch (sourceType) {
            case BANQUE -> imports.stream().mapToInt(i -> bankTransactionRepository.countByFileImportId(i.getId())).sum();
            case MOOV -> imports.stream().mapToInt(i -> moovTransactionRepository.countByFileImportId(i.getId())).sum();
            case ORANGE -> imports.stream().mapToInt(i -> orangeTransactionRepository.countByFileImportId(i.getId())).sum();
            case AMPLITUDE -> imports.stream().mapToInt(i -> amplitudeTransactionRepository.countByFileImportId(i.getId())).sum();
        };

        List<ReconciliationRun> impactedRuns = new ArrayList<>();
        for (ReconciliationRun run : reconciliationRunRepository.findAll()) {
            if (containsAnyImportId(run, sourceType, importIds)) {
                impactedRuns.add(run);
            }
        }
        int deletedResults = 0;
        for (ReconciliationRun run : impactedRuns) {
            deletedResults += reconciliationResultRepository.countByRunId(run.getId());
            reconciliationResultRepository.deleteByRunId(run.getId());
        }
        if (!impactedRuns.isEmpty()) {
            reconciliationRunRepository.deleteAll(impactedRuns);
        }

        switch (sourceType) {
            case BANQUE -> importIds.forEach(bankTransactionRepository::deleteByFileImportId);
            case MOOV -> importIds.forEach(moovTransactionRepository::deleteByFileImportId);
            case ORANGE -> importIds.forEach(orangeTransactionRepository::deleteByFileImportId);
            case AMPLITUDE -> importIds.forEach(amplitudeTransactionRepository::deleteByFileImportId);
        }
        for (FileImport fileImport : imports) {
            fileImportRepository.delete(fileImport);
            fileStorageService.deleteIfExists(fileImport.getFilePath());
        }
        return new DeletionStats(imports.size(), deletedTransactions, deletedResults, impactedRuns.size());
    }

    private record DeletionStats(int deletedImports, int deletedTransactions, int deletedResults, int deletedRuns) {
    }

    private String extractAmplitudeOperationReference(Map<String, String> row) {
        List<String> headers = new ArrayList<>(row.keySet());
        for (int i = 0; i < headers.size() - 1; i++) {
            String header = headers.get(i);
            if (header == null) continue;
            String h = header.trim().toLowerCase();
            if (!"libelle".equals(h) && !"libellé".equals(h)) continue;
            String candidate = row.get(headers.get(i + 1));
            if (candidate == null || candidate.isBlank()) return null;
            String digits = candidate.replaceAll("\\D", "");
            if (digits.length() < 10) return null;
            return digits.substring(0, 10);
        }
        return null;
    }

    private String normalizeAmplitudeReference(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        return digits.length() < 10 ? null : digits;
    }

    private java.time.LocalDateTime parseBankTransactionDate(Map<String, String> row) {
        String raw = ParseUtils.firstNonBlank(
                row.get("Date transaction"),
                row.get("Date Operation"),
                row.get("TXDATE_")
        );
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() >= 19) {
            value = value.substring(0, 19);
        }
        java.time.LocalDateTime parsed = ParseUtils.parseDateTime(value);
        if (parsed != null) {
            return parsed;
        }
        return ParseUtils.parseDateTime(raw);
    }

    private String normalizeAllocationStatusLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        return switch (raw.trim()) {
            case "Allocated" -> "Alloué";
            case "PaymentRejected" -> "Paiement rejeté";
            case "AllocationFailed" -> "Echec allocation";
            default -> raw;
        };
    }

    private String normalizeAmplitudePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.replace("et", " ").replace("ET", " ").trim();
        String digits = cleaned.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}

