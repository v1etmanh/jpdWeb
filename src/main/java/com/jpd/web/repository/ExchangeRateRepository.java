package com.jpd.web.repository;

import com.jpd.web.model.ExchangeRateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateConfig, Long> {
}