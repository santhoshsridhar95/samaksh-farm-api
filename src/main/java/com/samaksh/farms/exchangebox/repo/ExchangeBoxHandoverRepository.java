package com.samaksh.farms.exchangebox.repo;

import com.samaksh.farms.exchangebox.entity.ExchangeBoxHandover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeBoxHandoverRepository
        extends JpaRepository<ExchangeBoxHandover, Long> {

    List<ExchangeBoxHandover> findAllByOrderByReceivedAtDesc();
}
