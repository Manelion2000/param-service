package com.bakouan.app.controller;

import com.bakouan.app.model.BankTransaction;
import com.bakouan.app.model.MoovTransaction;
import com.bakouan.app.model.OrangeTransaction;
import com.bakouan.app.repositories.BankTransactionRepository;
import com.bakouan.app.repositories.MoovTransactionRepository;
import com.bakouan.app.repositories.OrangeTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final BankTransactionRepository bankTransactionRepository;
    private final MoovTransactionRepository moovTransactionRepository;
    private final OrangeTransactionRepository orangeTransactionRepository;

    @GetMapping("/bank")
    public Page<BankTransaction> bank(@RequestParam Long importId, Pageable pageable) {
        return bankTransactionRepository.findByFileImportId(importId, pageable);
    }

    @GetMapping("/moov")
    public Page<MoovTransaction> moov(@RequestParam Long importId,
                                      @RequestParam(required = false) String transactionType,
                                      Pageable pageable) {
        if (transactionType != null && !transactionType.isBlank()) {
            return moovTransactionRepository.findByFileImportIdAndTransactionTypeIgnoreCase(importId, transactionType, pageable);
        }
        return moovTransactionRepository.findByFileImportId(importId, pageable);
    }

    @GetMapping("/orange")
    public Page<OrangeTransaction> orange(@RequestParam Long importId, Pageable pageable) {
        return orangeTransactionRepository.findByFileImportId(importId, pageable);
    }

    @GetMapping("/bank/by-ids")
    public List<BankTransaction> bankByIds(@RequestParam(required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return bankTransactionRepository.findAllById(ids);
    }

    @PostMapping("/bank/by-ids")
    public List<BankTransaction> bankByIdsPost(@RequestBody(required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return bankTransactionRepository.findAllById(ids);
    }

    @GetMapping("/moov/by-ids")
    public List<MoovTransaction> moovByIds(@RequestParam(required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return moovTransactionRepository.findAllById(ids);
    }

    @PostMapping("/moov/by-ids")
    public List<MoovTransaction> moovByIdsPost(@RequestBody(required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return moovTransactionRepository.findAllById(ids);
    }

    @GetMapping("/orange/by-ids")
    public List<OrangeTransaction> orangeByIds(@RequestParam(required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return orangeTransactionRepository.findAllById(ids);
    }

    @PostMapping("/orange/by-ids")
    public List<OrangeTransaction> orangeByIdsPost(@RequestBody(required = false) List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return orangeTransactionRepository.findAllById(ids);
    }
}

