package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.PaymentTracking;


@Repository
public interface PaymentTrackingRepository extends JpaRepository<PaymentTracking, String> {
    List<PaymentTracking> findByStatus(String status);
}
