package com.coding.accountservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "account_transaction")
public class AccountTransaction {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

	@Id
	@Column(name = "event_id", nullable = false, updatable = false)
	private String eventId;

	@Column(nullable = false)
	private String accountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventType type;

	@NotNull(message = "amount is required")
	@DecimalMin(value = "0.01", inclusive = true, message = "amount must be greater than 0")
	private BigDecimal amount;

	@Column(nullable = false)
	private String currency;

	@Column(nullable = false)
	private Instant eventTimestamp;

	@Column(nullable = false)
	private Instant createdAt;

	@Lob
	@Column(name = "metadata_json")
	private String metadataJson;

	@PrePersist
	public void prePersist() {
		this.createdAt = Instant.now();
	}
//
//    public Long getId() {
//        return id;
//    }

	public String getEventId() {
		return eventId;
	}

	public String getAccountId() {
		return accountId;
	}

	public EventType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public Instant getEventTimestamp() {
		return eventTimestamp;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public void setEventTimestamp(Instant eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}

}