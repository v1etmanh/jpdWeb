package com.jpd.web.service.utils;

import com.jpd.web.model.ExchangeRateConfig;
import com.jpd.web.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExchangeRateService {

    @Autowired
    private ExchangeRateRepository repo;

    @Autowired
    private RestTemplate restTemplate;



    public double getUsdToVndRate() {
        return repo.findById(1L).orElseThrow().getUsdToVnd();
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    public void updateExchangeRate() {
        try {
            // API miễn phí, không cần key
            String url = "https://api.exchangerate-api.com/v4/latest/USD";
            Map response = restTemplate.getForObject(url, Map.class);

            Map<String, Double> rates = (Map<String, Double>) response.get("rates");
            double vndRate = rates.get("VND");

            // Update DB
            ExchangeRateConfig config = repo.findById(1L).orElseThrow();
            config.setUsdToVnd(vndRate);
            config.setUpdatedAt(LocalDateTime.now());
            repo.save(config);

            log.info("✅ Updated: 1 USD = {} VND", vndRate);

        } catch (Exception e) {
            log.error("❌ Update failed", e);
        }
    }
}