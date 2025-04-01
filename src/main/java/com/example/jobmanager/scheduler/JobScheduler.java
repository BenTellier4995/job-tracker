package com.example.jobmanager.scheduler;

import com.example.jobmanager.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobScheduler {

    private final JobService jobService;

    @Scheduled(fixedRate = 60000)
    public void scheduleJobFetch() throws IOException, GeneralSecurityException {
        System.out.println("TEST 1");

        jobService.fetchAndCreateJobsFromEmails();
        System.out.println("TEST 2");
    }
}
