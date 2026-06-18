package com.tungdt.microservices.background.controller;

import com.tungdt.microservices.background.dto.JobEventResponse;
import com.tungdt.microservices.background.service.BackgroundJobService;
import com.tungdt.microservices.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class BackgroundJobController {
    private final BackgroundJobService backgroundJobService;

    public BackgroundJobController(BackgroundJobService backgroundJobService) {
        this.backgroundJobService = backgroundJobService;
    }

    @GetMapping("/events")
    public ApiResponse<List<JobEventResponse>> getEvents() {
        return ApiResponse.ok(backgroundJobService.getAll());
    }
}
