package com.example.jobmanager.dto;


import com.example.jobmanager.model.ExperienceLevel;
import com.example.jobmanager.model.JobStatus;
import com.example.jobmanager.model.JobType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class JobDTO {
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
}
