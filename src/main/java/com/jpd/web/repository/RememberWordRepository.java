package com.jpd.web.repository;

import com.jpd.web.model.RememberWord;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RememberWordRepository extends JpaRepository<RememberWord,Long> {
    @Query("select r from RememberWord  r join Customer c on r.customer.customerId=c.customerId where c.email=:customerEmail")
    List<RememberWord> findAllByCustomer_Email(@Param("customerEmail") String customerEmail);
}
