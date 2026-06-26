package com.coding.accountservice.dto;

import java.math.BigDecimal;

public class ApplyTransactionResponse {

	private String accountId;
	private String eventId;
	private boolean duplicate;
	private String transactionStatus;

	public String getAccountId() {
		return accountId;
	}

	public String getEventId() {
		return eventId;
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

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}

	public String getTransactionStatus() {
		return transactionStatus;
	}

	public void setTransactionStatus(String transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

}