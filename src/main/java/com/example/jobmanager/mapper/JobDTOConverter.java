package com.example.jobmanager.mapper;

import com.example.jobmanager.dto.JobDTO;
import com.example.jobmanager.model.Job;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JobDTOConverter {

    private final ModelMapper modelMapper;

    public JobDTO convertToDTO(Job job) {
        return modelMapper.map(job, JobDTO.class);
    }

    public Job convertToEntity(JobDTO jobDTO) {

        Job job = modelMapper.map(jobDTO, Job.class);

        job.setCreatedAT(LocalDateTime.now().plusHours(2));
        job.setUpdatedAt(LocalDateTime.now().plusHours(2));

        return job;
    }
}
