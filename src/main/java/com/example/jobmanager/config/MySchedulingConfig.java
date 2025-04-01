package com.example.jobmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@EnableScheduling
@EnableAsync
@Configuration
@Slf4j
public class MySchedulingConfig {

    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(new ScheduledThreadPoolExecutor(20));
    }
}
