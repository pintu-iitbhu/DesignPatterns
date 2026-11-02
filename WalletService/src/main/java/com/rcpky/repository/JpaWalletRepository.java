package com.rcpky.repository;

import com.rcpky.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWalletRepository extends JpaRepository<Wallet, String>, IWalletRepository {
}
