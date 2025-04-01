package com.example.jobmanager.repository;

import com.example.jobmanager.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface JobRepository extends MongoRepository<Job, String> {

    boolean existsByCompanyNameAndJobTitle(String companyName, String jobTitle);
}
