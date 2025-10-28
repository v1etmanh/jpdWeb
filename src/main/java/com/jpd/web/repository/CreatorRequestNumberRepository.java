package com.jpd.web.repository;

import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.CreatorRequestNumber;
import java.util.List;
import java.util.Optional;


@Repository
public interface CreatorRequestNumberRepository  extends CrudRepository<CreatorRequestNumber, Long>{
Optional<CreatorRequestNumber>  findByCreatorId(long creatorId);
}
