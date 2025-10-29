package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Status;

import java.util.List;
import java.util.Optional;

public interface CreatorRepository extends JpaRepository<Creator, Long> {
       Optional<Creator> findByCustomer(Customer customer);

       @Query("SELECT COUNT(DISTINCT e) FROM Enrollment e " +
                     "JOIN e.course c " +
                     "WHERE c.creator.creatorId = :creatorId")
       int countTotalStudentsByCreatorId(@Param("creatorId") Long creatorId);

       @Query("SELECT AVG(f.rate) FROM Feedback f " +
                     "JOIN f.enrollment e " +
                     "JOIN e.course c " +
                     "WHERE c.creator.creatorId = :creatorId")
       Double getAverageRatingByCreatorId(@Param("creatorId") Long creatorId);

       // Admin management queries
       List<Creator> findAllByStatus(Status status);

       List<Creator> findByFullNameContainingIgnoreCase(String fullName);
}
