package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.RememberWord;

public interface RememberWordRepository extends CrudRepository<RememberWord,Long>{

	List<RememberWord> findAllByCustomer_Email(String email);

}
