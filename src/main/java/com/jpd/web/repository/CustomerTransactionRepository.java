package com.jpd.web.repository;

import com.jpd.web.model.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Repository
public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    // findTransactionsByEnrollmentId(bigint)
    List<CustomerTransaction> findByEnrollment_EnrollId(Long enrollId);

    // findTransactionsByCustomerId(bigint)
    List<CustomerTransaction> findByEnrollment_Customer_CustomerId(Long customerId);

    @Query("""
    SELECT SUM(t.amount) 
    FROM CustomerTransaction t 
    JOIN t.enrollment e 
    WHERE e.course.courseId = :courseId 
      AND t.status = 'SUCCESS'
""")
    Double getTotalRevenueByCourseId(@Param("courseId") Long courseId);



    // updateTransactionStatus(bigint, varchar)
    @Modifying
    @Transactional
    @Query("UPDATE CustomerTransaction t SET t.status = :status WHERE t.transactionID = :transactionId")
    int updateTransactionStatus(Long transactionId, String status);


}
