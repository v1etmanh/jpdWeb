package com.jpd.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.Creator_code_changeP;

public interface Creator_code_changePRepository extends CrudRepository<Creator_code_changeP, Long> {
	Optional<Creator_code_changeP> findFirstByCreatorIdOrderByCreaTimeDesc(long creatorId);}
