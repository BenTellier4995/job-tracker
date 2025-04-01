package com.example.jobmanager.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "my_positions")
public class Job {
    @Id
    private String id;
    private String companyName;
    private String jobTitle;
    private String jobUrl;
    private String appliedDate;
    private JobType jobType;
    private String category;
    private String salaryEstimate;
    private String location;
    private JobStatus status;
    private String notes;
    private String cvFileUrl;
    private ExperienceLevel experienceLevel;
    private LocalDateTime createdAT;
    private LocalDateTime updatedAt;
}
