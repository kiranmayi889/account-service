package com.coding.accountservice.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository transactionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AccountService service;

    @BeforeEach
    void setup() {

        ReflectionTestUtils.setField(
                service,
                "meterRegistry",
                new SimpleMeterRegistry());

        service.init();
    }

    private ApplyTransactionRequest request() {

        ApplyTransactionRequest request =
                new ApplyTransactionRequest();

        request.setEventId("EVT-100");
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("USD");
        request.setType(EventType.CREDIT);
        request.setEventTimestamp(Instant.now());
        request.setMetadata(Map.of("source", "ATM"));

        return request;
    }

    @Test
    void shouldProcessCreditTransaction() throws Exception {

        ApplyTransactionRequest request = request();

        Account account = new Account();
        account.setAccountId("ACC-1");
        account.setBalance(BigDecimal.valueOf(200));

        when(transactionRepository.findById("EVT-100"))
                .thenReturn(Optional.empty());

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.of(account));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}");

        ApplyTransactionResponse response =
                service.applyTransaction("ACC-1", request);

        assertNotNull(response);
        assertFalse(response.isDuplicate());
        assertEquals("PROCESSED",
                response.getTransactionStatus());

        assertEquals(
                BigDecimal.valueOf(300),
                account.getBalance());

        verify(transactionRepository)
                .save(any(AccountTransaction.class));

        verify(accountRepository)
                .save(account);
    }

    @Test
    void shouldProcessDebitTransaction() throws Exception {

        ApplyTransactionRequest request = request();
        request.setType(EventType.DEBIT);
        request.setAmount(BigDecimal.valueOf(50));

        Account account = new Account();
        account.setAccountId("ACC-1");
        account.setBalance(BigDecimal.valueOf(200));

        when(transactionRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        when(accountRepository.findById(anyString()))
                .thenReturn(Optional.of(account));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}");

        ApplyTransactionResponse response =
                service.applyTransaction("ACC-1", request);

        assertEquals(
                BigDecimal.valueOf(150),
                account.getBalance());

        assertEquals(
                "PROCESSED",
                response.getTransactionStatus());

        verify(accountRepository).save(account);
    }

    @Test
    void shouldReturnDuplicateWhenEventAlreadyProcessed() {

        AccountTransaction existing =
                new AccountTransaction();

        existing.setEventId("EVT-100");

        Account account = new Account();
        account.setAccountId("ACC-1");
        account.setBalance(BigDecimal.valueOf(500));

        when(transactionRepository.findById("EVT-100"))
                .thenReturn(Optional.of(existing));

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.of(account));

        ApplyTransactionResponse response =
                service.applyTransaction("ACC-1", request());

        assertTrue(response.isDuplicate());

        assertEquals(
                "PROCESSED",
                response.getTransactionStatus());

        verify(transactionRepository, never())
                .save(any(AccountTransaction.class));

        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void shouldCreateAccountIfNotExists() throws Exception {

        when(transactionRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        when(accountRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}");

        ApplyTransactionResponse response =
                service.applyTransaction("ACC-1", request());

        assertNotNull(response);

        assertEquals(
                "PROCESSED",
                response.getTransactionStatus());

        verify(accountRepository)
                .save(any(Account.class));

        verify(transactionRepository)
                .save(any(AccountTransaction.class));
    }
    
    @Test
    void shouldReturnBalance() {

        Account account = new Account();
        account.setAccountId("ACC-1");
        account.setBalance(BigDecimal.valueOf(1000));

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.of(account));

        BalanceResponse response = service.getBalance("ACC-1");

        assertNotNull(response);
        assertEquals("ACC-1", response.getAccountId());
        assertEquals(BigDecimal.valueOf(1000), response.getBalance());

        verify(accountRepository).findById("ACC-1");
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFoundForBalance() {

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.getBalance("ACC-1"));

        verify(accountRepository).findById("ACC-1");
    }

    @Test
    void shouldReturnAccountDetails() throws Exception {

        Account account = new Account();
        account.setAccountId("ACC-1");
        account.setBalance(BigDecimal.valueOf(500));
        account.setUpdatedAt(Instant.now());

        AccountTransaction tx = new AccountTransaction();
        tx.setEventId("EVT-1");
        tx.setAccountId("ACC-1");
        tx.setType(EventType.CREDIT);
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setCurrency("USD");
        tx.setEventTimestamp(Instant.now());
        tx.setMetadataJson("{\"source\":\"ATM\"}");

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findTop10ByAccountIdOrderByEventTimestampDesc("ACC-1"))
                .thenReturn(List.of(tx));

        when(objectMapper.readValue(
                anyString(),
                any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("source", "ATM"));

        AccountDetailsResponse response =
                service.getAccountDetails("ACC-1");

        assertNotNull(response);
        assertEquals("ACC-1", response.getAccountId());
        assertEquals(BigDecimal.valueOf(500), response.getBalance());
        assertEquals(1, response.getRecentTransactions().size());

        AccountTransactionDto dto =
                response.getRecentTransactions().get(0);

        assertEquals("EVT-1", dto.getEventId());
        assertEquals(BigDecimal.valueOf(100), dto.getAmount());
        assertEquals("ATM", dto.getMetadata().get("source"));
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFoundForAccountDetails() {

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.getAccountDetails("ACC-1"));
    }

    @Test
    void shouldThrowExceptionWhenMetadataIsInvalid() throws Exception {

        Account account = new Account();
        account.setAccountId("ACC-1");

        AccountTransaction tx = new AccountTransaction();
        tx.setMetadataJson("INVALID_JSON");

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findTop10ByAccountIdOrderByEventTimestampDesc("ACC-1"))
                .thenReturn(List.of(tx));

        when(objectMapper.readValue(
                anyString(),
                any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new RuntimeException("Invalid JSON"));

        assertThrows(IllegalStateException.class,
                () -> service.getAccountDetails("ACC-1"));
    }

    @Test
    void shouldReturnEmptyTransactionsWhenNoTransactionsExist() {

        Account account = new Account();
        account.setAccountId("ACC-1");
        account.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findTop10ByAccountIdOrderByEventTimestampDesc("ACC-1"))
                .thenReturn(Collections.emptyList());

        AccountDetailsResponse response =
                service.getAccountDetails("ACC-1");

        assertNotNull(response);
        assertTrue(response.getRecentTransactions().isEmpty());
    }
    
    @Test
    void shouldReturnTransactionsInDescendingTimestampOrder() throws Exception {

        Account account = new Account();
        account.setAccountId("ACC-1");
        account.setBalance(BigDecimal.valueOf(500));

        AccountTransaction latest = new AccountTransaction();
        latest.setEventId("EVT-2");
        latest.setEventTimestamp(Instant.parse("2026-06-20T12:00:00Z"));
        latest.setMetadataJson("{}");

        AccountTransaction oldest = new AccountTransaction();
        oldest.setEventId("EVT-1");
        oldest.setEventTimestamp(Instant.parse("2026-06-18T12:00:00Z"));
        oldest.setMetadataJson("{}");

        when(accountRepository.findById("ACC-1"))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findTop10ByAccountIdOrderByEventTimestampDesc("ACC-1"))
                .thenReturn(List.of(latest, oldest));

        when(objectMapper.readValue(
                anyString(),
                any(TypeReference.class)))
                .thenReturn(Map.of());

        AccountDetailsResponse response =
                service.getAccountDetails("ACC-1");

        assertEquals(
                "EVT-2",
                response.getRecentTransactions().get(0).getEventId());

        assertEquals(
                "EVT-1",
                response.getRecentTransactions().get(1).getEventId());
    }
}