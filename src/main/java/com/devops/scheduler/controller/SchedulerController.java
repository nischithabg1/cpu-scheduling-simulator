package com.devops.scheduler.controller;

import com.devops.scheduler.model.ScheduleRequest;
import com.devops.scheduler.model.ScheduleResponse;
import com.devops.scheduler.service.SchedulerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SchedulerController {

    private final SchedulerService service;

    public SchedulerController(SchedulerService service) {
        this.service = service;
    }

    /** GET /algorithms - list available algorithms. */
    @GetMapping("/algorithms")
    public List<String> algorithms() {
        return service.algorithms();
    }

    /** POST /schedule/{fcfs|sjf|round-robin|priority} */
    @PostMapping("/schedule/{algorithm}")
    public ScheduleResponse schedule(@PathVariable String algorithm,
                                     @RequestBody ScheduleRequest request) {
        return service.run(algorithm, request);
    }
}
