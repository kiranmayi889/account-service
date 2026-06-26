package com.coding.accountservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.coding.accountservice.entity.AccountTransaction;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, String> {

    Optional<AccountTransaction> findByEventId(String eventId);

    List<AccountTransaction> findTop10ByAccountIdOrderByEventTimestampDesc(String accountId);
}