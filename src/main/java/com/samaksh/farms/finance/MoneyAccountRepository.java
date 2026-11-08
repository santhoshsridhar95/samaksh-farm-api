package com.samaksh.farms.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MoneyAccountRepository extends JpaRepository<MoneyAccount, Long> {
    Optional<MoneyAccount> findByAccountKey(String key);
}
