package com.coding.accountservice.dto;

import java.math.BigDecimal;

public class ApplyTransactionResponse {

    private String accountId;
    private String eventId;
    private BigDecimal balance;
    private boolean duplicate;

    public String getAccountId() {
        return accountId;
    }

    public String getEventId() {
        return eventId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }
}