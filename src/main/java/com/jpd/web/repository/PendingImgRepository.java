package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;

@Repository
public interface PendingImgRepository extends CrudRepository<PendingImage, Long>{
List<PendingImage> findByStatus(Status status);
}
