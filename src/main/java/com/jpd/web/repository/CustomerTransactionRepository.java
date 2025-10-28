package com.jpd.web.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.jpd.web.model.CustomerTransaction;
@Repository
public interface CustomerTransactionRepository  extends CrudRepository<CustomerTransaction,Long>{
	

}
