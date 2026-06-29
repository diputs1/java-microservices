package com.tungdt.microservices.background.controller;

import com.tungdt.microservices.background.dto.JobEventResponse;
import com.tungdt.microservices.background.dto.DlqReplayResponse;
import com.tungdt.microservices.background.service.BackgroundJobService;
import com.tungdt.microservices.background.service.DlqReplayService;
import com.tungdt.microservices.common.api.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class BackgroundJobController {
    private final BackgroundJobService backgroundJobService;
    private final DlqReplayService dlqReplayService;

    public BackgroundJobController(BackgroundJobService backgroundJobService, DlqReplayService dlqReplayService) {
        this.backgroundJobService = backgroundJobService;
        this.dlqReplayService = dlqReplayService;
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<JobEventResponse>> getEvents() {
        return ApiResponse.ok(backgroundJobService.getAll());
    }

    @PostMapping("/dlq/{queueName}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DlqReplayResponse> replayDlq(@PathVariable String queueName,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(dlqReplayService.replay(queueName, limit));
    }
}
