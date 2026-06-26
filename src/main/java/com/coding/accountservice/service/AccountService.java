package com.coding.accountservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coding.accountservice.dto.AccountDetailsResponse;
import com.coding.accountservice.dto.AccountTransactionDto;
import com.coding.accountservice.dto.ApplyTransactionRequest;
import com.coding.accountservice.dto.ApplyTransactionResponse;
import com.coding.accountservice.dto.BalanceResponse;
import com.coding.accountservice.entity.Account;
import com.coding.accountservice.entity.AccountTransaction;
import com.coding.accountservice.entity.EventType;
import com.coding.accountservice.exception.AccountNotFoundException;
import com.coding.accountservice.repository.AccountRepository;
import com.coding.accountservice.repository.AccountTransactionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AccountService {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private AccountTransactionRepository transactionRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Transactional
	// used serializable isolation to handle multiple requests so that in case if
	// multiple threads came the balance should be updated appropriately
	public ApplyTransactionResponse applyTransaction(String accountId, ApplyTransactionRequest request) {
		// 1) If event already exists, it's a duplicate/idempotent retry
		AccountTransaction existing = transactionRepository.findById(request.getEventId()).orElse(null);
		Account account;
		if (existing != null) {
			account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));

			ApplyTransactionResponse response = new ApplyTransactionResponse();
			response.setAccountId(accountId);
			response.setEventId(existing.getEventId());
			response.setDuplicate(true);
			response.setTransactionStatus("PROCESSED");
			return response;
		} else {
			account = accountRepository.findById(accountId).orElse(null);
		}

		if (account == null) {
			account = new Account();
			account.setAccountId(accountId);
			account.setBalance(BigDecimal.ZERO);
		}
		// 3) Insert transaction row first.
		// eventId is the idempotency key. If another thread inserts same eventId
		// concurrently,
		// this save will fail and we return duplicate=true.
		AccountTransaction transaction = new AccountTransaction();
		transaction.setEventId(request.getEventId());
		transaction.setAccountId(accountId);
		transaction.setType(request.getType());
		transaction.setAmount(request.getAmount());
		transaction.setCurrency(request.getCurrency());
		transaction.setEventTimestamp(request.getEventTimestamp());
		transaction.setMetadataJson(toJson(request.getMetadata()));
		ApplyTransactionResponse response = new ApplyTransactionResponse();
		try {
			transactionRepository.save(transaction);
		} catch (Exception ex) {
			response.setAccountId(accountId);
			response.setEventId(request.getEventId());
			response.setTransactionStatus("FAILED");
			response.setDuplicate(true);
		}

		if (request.getType() == EventType.CREDIT) {
			account.setBalance(account.getBalance().add(request.getAmount()));
		} else {
			account.setBalance(account.getBalance().subtract(request.getAmount()));
		}

		account.setUpdatedAt(Instant.now());
		try {
			accountRepository.save(account);
			response.setAccountId(accountId);
			response.setEventId(request.getEventId());
			response.setDuplicate(false);
			response.setTransactionStatus("PROCESSED");

		} catch (Exception ex) {
			response.setAccountId(accountId);
			response.setEventId(request.getEventId());
			response.setTransactionStatus("FAILED");
			response.setDuplicate(true);
		}
		return response;
	}

	public BalanceResponse getBalance(String accountId) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));
		return new BalanceResponse(account.getAccountId(), account.getBalance());
	}

	public AccountDetailsResponse getAccountDetails(String accountId) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));

		List<AccountTransactionDto> recentTransactions = transactionRepository
				.findTop10ByAccountIdOrderByEventTimestampDesc(accountId).stream().map(this::toDto).toList();

		AccountDetailsResponse response = new AccountDetailsResponse();
		response.setAccountId(account.getAccountId());
		response.setBalance(account.getBalance());
		response.setUpdatedAt(account.getUpdatedAt());
		response.setRecentTransactions(recentTransactions);
		return response;
	}

	private AccountTransactionDto toDto(AccountTransaction tx) {
		AccountTransactionDto dto = new AccountTransactionDto();
		dto.setEventId(tx.getEventId());
		dto.setType(tx.getType());
		dto.setAmount(tx.getAmount());
		dto.setCurrency(tx.getCurrency());
		dto.setEventTimestamp(tx.getEventTimestamp());
		dto.setMetadata(fromJson(tx.getMetadataJson()));
		return dto;
	}

	private String toJson(Map<String, Object> metadata) {
		try {
			return metadata == null ? null : objectMapper.writeValueAsString(metadata);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to serialize metadata", ex);
		}
	}

	private Map<String, Object> fromJson(String metadataJson) {
		try {
			if (metadataJson == null || metadataJson.isBlank()) {
				return Collections.emptyMap();
			}
			return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to deserialize metadata", ex);
		}
	}
}