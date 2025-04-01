package com.example.jobmanager.controller;


import com.example.jobmanager.dto.JobDTO;
import com.example.jobmanager.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public JobDTO createJob(@RequestBody JobDTO jobDTO) {
        return jobService.createJob(jobDTO);
    }

    @GetMapping("/{id}")
    public JobDTO getJob(@PathVariable String id) {
        return jobService.getJob(id);
    }

    @PutMapping("/update/{id}")
    public JobDTO updateJob(@PathVariable String id, @RequestBody JobDTO jobDTO) {
        return jobService.updateJob(id, jobDTO);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteJob(@PathVariable String id) {
        jobService.removeTask(id);
    }

    @GetMapping("/getAll")
    public List<JobDTO> getAllJobs() {
        return jobService.getAllJobs();
    }

    @PostMapping("/fetch-and-create")
    public void fetchAndCreateJobs() throws IOException, GeneralSecurityException {
        jobService.fetchAndCreateJobsFromEmails();
    }
}
