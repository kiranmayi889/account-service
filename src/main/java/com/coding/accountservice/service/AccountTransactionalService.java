package com.coding.accountservice.service;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coding.accountservice.dto.ApplyTransactionRequest;
import com.coding.accountservice.dto.ApplyTransactionResponse;
import com.coding.accountservice.entity.Account;
import com.coding.accountservice.entity.AccountTransaction;
import com.coding.accountservice.entity.EventType;
import com.coding.accountservice.repository.AccountRepository;
import com.coding.accountservice.repository.AccountTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AccountTransactionalService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public ApplyTransactionResponse applyTransactionOnce(String accountId, ApplyTransactionRequest request) {
        // 1) duplicate event check
        AccountTransaction existing = transactionRepository.findById(request.getEventId()).orElse(null);
        if (existing != null) {
            Account existingAccount = accountRepository.findById(existing.getAccountId())
                    .orElseThrow(() -> new IllegalStateException("Account missing for duplicate event"));

            ApplyTransactionResponse response = new ApplyTransactionResponse();
            response.setAccountId(existingAccount.getAccountId());
            response.setEventId(existing.getEventId());
            response.setBalance(existingAccount.getBalance());
            response.setDuplicate(true);
            return response;
        }

        // 2) load or create account
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            try {
                account = new Account();
                account.setAccountId(accountId);
                account.setBalance(BigDecimal.ZERO);
                //account.setUpdatedAt(Instant.now());
                account = accountRepository.saveAndFlush(account);
            } catch (DataIntegrityViolationException ex) {
                // another thread created account concurrently
                account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalStateException("Failed to load account after concurrent create"));
            }
            catch(PessimisticLockingFailureException e)
            {
            	//another thread already created row
            }
        }

        // 3) insert transaction row (idempotency key = eventId)
        AccountTransaction transaction = new AccountTransaction();
        transaction.setEventId(request.getEventId());
        transaction.setAccountId(accountId);
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setEventTimestamp(request.getEventTimestamp());
        transaction.setMetadataJson(toJson(request.getMetadata()));

        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException ex) {
            // same event inserted concurrently by another thread
            ApplyTransactionResponse response = new ApplyTransactionResponse();
            response.setAccountId(account.getAccountId());
            response.setEventId(request.getEventId());
            response.setBalance(account.getBalance());
            response.setDuplicate(true);
            return response;
        }

        // 4) update balance
        if (request.getType() == EventType.CREDIT) {
            account.setBalance(account.getBalance().add(request.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(request.getAmount()));
        }
        //account.setUpdatedAt(Instant.now());

        // version check happens here
        accountRepository.saveAndFlush(account);

        ApplyTransactionResponse response = new ApplyTransactionResponse();
        response.setAccountId(account.getAccountId());
        response.setEventId(request.getEventId());
        response.setBalance(account.getBalance());
        response.setDuplicate(false);
        return response;
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return metadata == null ? null : objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize metadata", ex);
        }
    }
}