package com.jpd.web.repository;

import com.jpd.web.model.Creator;
import com.jpd.web.model.Wishlist;
import com.jpd.web.model.Withdraw;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface WithdrawRepository extends CrudRepository<Withdraw,Long> {
    Optional<Withdraw> findByPayoutBatchId(String payoutBatchId); 
    List<Withdraw>  findByCreator(Creator creator);
   
}
