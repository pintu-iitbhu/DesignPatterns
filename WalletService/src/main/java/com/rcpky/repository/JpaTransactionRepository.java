package com.rcpky.repository;

import com.rcpky.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaTransactionRepository extends JpaRepository<Transaction, String>, ITransactionRepository {

    @Query("SELECT t FROM Transaction t WHERE t.walletId = :walletId ORDER BY t.timestamp DESC")
    List<Transaction> findByWalletId(@Param("walletId") String walletId);
}
