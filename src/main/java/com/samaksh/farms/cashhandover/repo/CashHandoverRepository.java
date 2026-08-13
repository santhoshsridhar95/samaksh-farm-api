package com.samaksh.farms.cashhandover.repo;

import com.samaksh.farms.cashhandover.entity.CashHandover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashHandoverRepository
        extends JpaRepository<CashHandover, Long> {

    List<CashHandover> findAllByOrderByHandedOverAtDesc();
}
