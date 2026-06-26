package com.coding.accountservice.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coding.accountservice.dto.AccountDetailsResponse;
import com.coding.accountservice.dto.ApplyTransactionRequest;
import com.coding.accountservice.dto.ApplyTransactionResponse;
import com.coding.accountservice.dto.BalanceResponse;
import com.coding.accountservice.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Account API", description = "Operations related to Account Service")
public class AccountController {

	@Autowired
    private AccountService accountService;

    @PostMapping("/{accountId}/transactions")
    @Operation(summary = "Submit an Event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event Created"),
            @ApiResponse(responseCode = "400", description = "Invalid Request"),
            @ApiResponse(responseCode = "404", description = "Account Information Unavailable")
    })
    public ResponseEntity<ApplyTransactionResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody ApplyTransactionRequest request) {
        return ResponseEntity.ok(accountService.applyTransaction(accountId, request));
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get Account Balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "List Events by Account")
    public ResponseEntity<AccountDetailsResponse> getAccountDetails(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getAccountDetails(accountId));
    }
}