package com.jpd.web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.Creator;
import com.jpd.web.model.PayoutTracking;

public interface PayoutTrackingRepository extends CrudRepository<PayoutTracking, String> {
	List<PayoutTracking> findByStatus(String status);
    List<PayoutTracking> findByRecipientEmail(String email);

    // THÊM MỚI
    List<PayoutTracking> findByCreator(Creator creator);
    Optional<PayoutTracking> findByPayoutBatchId(String payoutBatchId);
    List<PayoutTracking> findAllByOrderByCreatedAtDesc();
    List<PayoutTracking> findByRecipientEmailOrderByCreatedAtDesc(String email);
}
