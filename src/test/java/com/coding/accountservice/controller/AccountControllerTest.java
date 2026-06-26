package com.coding.accountservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coding.accountservice.dto.AccountDetailsResponse;
import com.coding.accountservice.dto.ApplyTransactionResponse;
import com.coding.accountservice.dto.BalanceResponse;
import com.coding.accountservice.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldApplyTransaction() throws Exception {

        ApplyTransactionResponse response = new ApplyTransactionResponse();
        response.setAccountId("ACC-1");
        response.setEventId("EVT-1");
        response.setDuplicate(false);
        response.setTransactionStatus("PROCESSED");

        when(accountService.applyTransaction(eq("ACC-1"), any()))
                .thenReturn(response);

        String request = """
                {
                  "eventId":"EVT-1",
                  "amount":100,
                  "currency":"USD",
                  "type":"CREDIT",
                  "eventTimestamp":"2026-06-27T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/accounts/ACC-1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-1"))
                .andExpect(jsonPath("$.eventId").value("EVT-1"))
                .andExpect(jsonPath("$.transactionStatus").value("PROCESSED"));

        verify(accountService).applyTransaction(eq("ACC-1"), any());
    }

    @Test
    void shouldReturnBalance() throws Exception {

        BalanceResponse response =
                new BalanceResponse("ACC-1", BigDecimal.valueOf(500));

        when(accountService.getBalance("ACC-1"))
                .thenReturn(response);

        mockMvc.perform(get("/accounts/ACC-1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-1"))
                .andExpect(jsonPath("$.balance").value(500));
    }

    @Test
    void shouldReturnAccountDetails() throws Exception {

        AccountDetailsResponse response = new AccountDetailsResponse();
        response.setAccountId("ACC-1");
        response.setBalance(BigDecimal.valueOf(1000));

        when(accountService.getAccountDetails("ACC-1"))
                .thenReturn(response);

        mockMvc.perform(get("/accounts/ACC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-1"))
                .andExpect(jsonPath("$.balance").value(1000));
    }
}