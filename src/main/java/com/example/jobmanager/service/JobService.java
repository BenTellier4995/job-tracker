package com.example.jobmanager.service;

import com.example.jobmanager.dto.JobDTO;
import com.example.jobmanager.mapper.JobDTOConverter;
import com.example.jobmanager.model.ExperienceLevel;
import com.example.jobmanager.model.Job;
import com.example.jobmanager.model.JobStatus;
import com.example.jobmanager.repository.JobRepository;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobDTOConverter jobDTOConverter;
    private final JobRepository jobRepository;
    private final GmailService gmailService;

    private static final String JOB_EXISTING_MESSAGE = "The job: %s is already exist in your job board";
    private static final String JOB_DOES_NOT_EXISTING_MASSAGE = "The job: %s  doesn't exists in your job board";
    private static final String INVALID_APPLIED_DATE_FORMAT = "Applied date format is invalid. Expected format: dd/MM/yyyy HH:mm";

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4} (0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$"
    );

    private static final Pattern JOB_TITLE_PATTERN = Pattern.compile("(?i)(software\\s?(developer|engineer)|back[-\\s]?end\\s?(developer|engineer)|front[-\\s]?end\\s?(developer|engineer)|full[-\\s]?stack\\s?(developer|engineer))");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w\\.-]+(?:/[\\w\\.-]+)*");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("(?i)(?<=Location:|City:)(.*?)(?=\n|$)");
    private static final Pattern STATUS_PATTERN_APPLIED = Pattern.compile("\\b(apply|applying|applied)\\b");
    private static final Pattern STATUS_PATTERN_PENDING = Pattern.compile("\\b(pending|under review)\\b");
    private static final Pattern STATUS_PATTERN_INTERVIEW = Pattern.compile("\\b(interview|interviewing)\\b");
    private static final Pattern STATUS_PATTERN_ACCEPTED = Pattern.compile("\\b(accept|accepted|acceptance)\\b");
    private static final Pattern STATUS_PATTERN_REJECTED = Pattern.compile("\\b(unfortunately|rejected|declined|not selected)\\b");
    private static final Pattern EXPERIENCE_LEVEL_PATTERN_ENTRY = Pattern.compile("\\b(entry level|entry-level)\\b");
    private static final Pattern EXPERIENCE_LEVEL_PATTERN_JUNIOR = Pattern.compile("\\b(junior)\\b");
    private static final Pattern EXPERIENCE_LEVEL_PATTERN_MID = Pattern.compile("\\b(mid level|mid-level)\\b");
    private static final Pattern EXPERIENCE_LEVEL_PATTERN_SENIOR = Pattern.compile("\\b(senior)\\b");
    private static final Pattern EXPERIENCE_LEVEL_PATTERN_LEAD = Pattern.compile("\\b(lead)\\b");
    private static final Pattern EXPERIENCE_LEVEL_PATTERN_MANAGER = Pattern.compile("\\b(manager)\\b");
    private static final Pattern EXPERIENCE_LEVEL_PATTERN_DIRECTOR = Pattern.compile("\\b(director)\\b");
    private static final Pattern EXPERIENCE_LEVEL_PATTERN_EXECUTIVE = Pattern.compile("\\b(executive)\\b");


    public synchronized JobDTO createJob(JobDTO jobDTO) {
        if (jobRepository.existsByCompanyNameAndJobTitle(jobDTO.getCompanyName(), jobDTO.getJobTitle())) {
            throw new IllegalArgumentException(String.format(JOB_EXISTING_MESSAGE, jobDTO.getJobTitle()));
        }

        if (!isValidDateFormat(jobDTO.getAppliedDate())) {
            throw new IllegalArgumentException(String.format(INVALID_APPLIED_DATE_FORMAT));
        }

        Job job = jobDTOConverter.convertToEntity(jobDTO);
        job = jobRepository.save(job);

        return jobDTOConverter.convertToDTO(job);
    }

    public synchronized void fetchAndCreateJobsFromEmails() throws IOException, GeneralSecurityException {
        List<Message> messages = gmailService.fetchEmails();
        for (Message message : messages) {
            JobDTO jobDTO = createJobDTOFromEmail(message);
            createJob(jobDTO);
        }
    }

    private JobDTO createJobDTOFromEmail(Message message) {
        String snippet = message.getSnippet();
        String companyName = extractCompanyNameFromMessage(message);
        String jobTitle = extractJobTitleFromMessage(snippet);
        String appliedDate = extractAppliedDateFromMessage(message);
        String jobUrl = extractJobUrlFromMessage(snippet);
        String location = extractLocationFromMessage(snippet);
        String status = extractJobStatusFromMessage(snippet);
        String experienceLevel = extractExperienceLevelFromMessage(snippet);

        JobDTO jobDTO = new JobDTO();
        jobDTO.setCompanyName(companyName);
        jobDTO.setJobTitle(jobTitle);
        jobDTO.setAppliedDate(appliedDate);
        jobDTO.setJobUrl(jobUrl);
        jobDTO.setLocation(location);
        jobDTO.setStatus(JobStatus.valueOf(status.replace(" ", "_").toUpperCase()));
        jobDTO.setExperienceLevel(ExperienceLevel.valueOf(experienceLevel.replace(" ", "_").toUpperCase()));

        return jobDTO;
    }

    private String extractCompanyNameFromMessage(Message message) {
        if (message == null || message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return null;
        }

        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        for (MessagePartHeader header : headers) {
            if ("From".equalsIgnoreCase(header.getName())) {
                String from = header.getValue();

                if (from.contains("<") && from.contains(">")) {
                    return from.substring(0, from.indexOf("<")).trim();
                }

                if (from.contains("@")) {
                    return from.substring(from.indexOf("@") + 1, from.lastIndexOf("."));
                }

                return from;
            }
        }

        return null;
    }

    private String extractJobTitleFromMessage(String snippet) {
        if (snippet == null) return "UNKNOWN";
        Matcher matcher = JOB_TITLE_PATTERN.matcher(snippet);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }

        return "UNKNOWN";
    }

    private String extractAppliedDateFromMessage(Message message) {
        if (message == null) return "Unknown Date";

        try {
            long timestamp = message.getInternalDate();
            Date date = new Date(timestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Jerusalem"));

            return outputFormat.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJobUrlFromMessage(String snippet) {
        if (snippet == null) return "Unknown URL";
        Matcher matcher = URL_PATTERN.matcher(snippet);
        return matcher.find() ? matcher.group(0) : "Unknown URL";
    }

    private String extractLocationFromMessage(String snippet) {
        if (snippet == null) return "Unknown Location";
        Matcher matcher = LOCATION_PATTERN.matcher(snippet);
        return matcher.find() ? matcher.group(1).trim() : "Unknown Location";
    }

    private String extractJobStatusFromMessage(String snippet) {
        if (snippet == null) return JobStatus.PENDING.name();

        snippet = snippet.toLowerCase();

        if (STATUS_PATTERN_APPLIED.matcher(snippet).find()) {
            return JobStatus.APPLIED.name();
        } else if (STATUS_PATTERN_PENDING.matcher(snippet).find()) {
            return JobStatus.PENDING.name();
        } else if (STATUS_PATTERN_INTERVIEW.matcher(snippet).find()) {
            return JobStatus.INTERVIEW.name();
        } else if (STATUS_PATTERN_ACCEPTED.matcher(snippet).find()) {
            return JobStatus.ACCEPTED.name();
        } else if (STATUS_PATTERN_REJECTED.matcher(snippet).find()) {
            return JobStatus.REJECTED.name();
        }

        return JobStatus.PENDING.name();
    }

    private String extractExperienceLevelFromMessage(String content) {
        if (content == null) return "UNKNOWN";

        content = content.toLowerCase();

        if (EXPERIENCE_LEVEL_PATTERN_ENTRY.matcher(content).find()) {
            return ExperienceLevel.ENTRY_LEVEL.name();
        } else if (EXPERIENCE_LEVEL_PATTERN_JUNIOR.matcher(content).find()) {
            return ExperienceLevel.JUNIOR.name();
        } else if (EXPERIENCE_LEVEL_PATTERN_MID.matcher(content).find()) {
            return ExperienceLevel.MID_LEVEL.name();
        } else if (EXPERIENCE_LEVEL_PATTERN_SENIOR.matcher(content).find()) {
            return ExperienceLevel.SENIOR.name();
        } else if (EXPERIENCE_LEVEL_PATTERN_LEAD.matcher(content).find()) {
            return ExperienceLevel.LEAD.name();
        } else if (EXPERIENCE_LEVEL_PATTERN_MANAGER.matcher(content).find()) {
            return ExperienceLevel.MANAGER.name();
        } else if (EXPERIENCE_LEVEL_PATTERN_DIRECTOR.matcher(content).find()) {
            return ExperienceLevel.DIRECTOR.name();
        } else if (EXPERIENCE_LEVEL_PATTERN_EXECUTIVE.matcher(content).find()) {
            return ExperienceLevel.EXECUTIVE.name();
        }

        return "UNKNOWN";
    }

    public JobDTO getJob(String id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(String.format(JOB_DOES_NOT_EXISTING_MASSAGE, id)));

        return jobDTOConverter.convertToDTO(job);
    }

    public synchronized JobDTO updateJob(String id, JobDTO updatedJob) {
        Optional<Job> optionalJob = jobRepository.findById(id);
        if (optionalJob.isPresent()) {
            Job job = optionalJob.get();
            job.setCompanyName(updatedJob.getCompanyName());
            job.setJobTitle(updatedJob.getJobTitle());
            job.setJobUrl(updatedJob.getJobUrl());
            job.setAppliedDate(updatedJob.getAppliedDate());
            job.setJobType(updatedJob.getJobType());
            job.setCategory(updatedJob.getCategory());
            job.setSalaryEstimate(updatedJob.getSalaryEstimate());
            job.setLocation(updatedJob.getLocation());
            job.setStatus(updatedJob.getStatus());
            job.setNotes(updatedJob.getNotes());
            job.setCvFileUrl(updatedJob.getCvFileUrl());
            job.setExperienceLevel(updatedJob.getExperienceLevel());

            if (!isValidDateFormat(updatedJob.getAppliedDate())) {
                throw new IllegalArgumentException(String.format(INVALID_APPLIED_DATE_FORMAT));
            } else {
                job.setAppliedDate(updatedJob.getAppliedDate());
            }

            Job savedJob = jobRepository.save(job);
            return jobDTOConverter.convertToDTO(savedJob);
        }
        throw new IllegalArgumentException(String.format(JOB_DOES_NOT_EXISTING_MASSAGE, id));
    }

    public List<JobDTO> getAllJobs() {
        return jobRepository.findAll().stream().map(jobDTOConverter::convertToDTO).collect(Collectors.toList());
    }

    public synchronized void removeTask(String id) {
        if (!jobRepository.existsById(id)) {
            throw new IllegalArgumentException(String.format(JOB_DOES_NOT_EXISTING_MASSAGE, id));
        }
        jobRepository.deleteById(id);
    }

    private boolean isValidDateFormat(String date) {
        if (date == null || date.isEmpty()) return false;
        Matcher matcher = DATE_PATTERN.matcher(date);
        return matcher.matches();
    }
}
