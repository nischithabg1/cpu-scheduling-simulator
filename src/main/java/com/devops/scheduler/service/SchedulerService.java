package com.devops.scheduler.service;

import com.devops.scheduler.model.ProcessInput;
import com.devops.scheduler.model.ScheduleRequest;
import com.devops.scheduler.model.ScheduleResponse;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SchedulerService {

    private final Map<String, Scheduler> schedulers = new LinkedHashMap<>();

    public SchedulerService(List<Scheduler> available) {
        available.stream()
                .sorted((a, b) -> a.key().compareTo(b.key()))
                .forEach(s -> schedulers.put(s.key(), s));
    }

    public List<String> algorithms() {
        return List.copyOf(schedulers.keySet());
    }

    public ScheduleResponse run(String algorithm, ScheduleRequest request) {
        Scheduler scheduler = schedulers.get(algorithm.toLowerCase());
        if (scheduler == null) {
            throw new IllegalArgumentException("Unknown algorithm '" + algorithm
                    + "'. Available: " + algorithms());
        }
        validate(request);
        int quantum = request.timeQuantum() == null ? 2 : request.timeQuantum();
        if ("round-robin".equals(scheduler.key()) && quantum <= 0) {
            throw new IllegalArgumentException("timeQuantum must be greater than 0");
        }
        return scheduler.schedule(request.processes(), quantum);
    }

    private void validate(ScheduleRequest request) {
        if (request == null || request.processes() == null || request.processes().isEmpty()) {
            throw new IllegalArgumentException("At least one process is required");
        }
        Set<String> ids = new HashSet<>();
        for (ProcessInput p : request.processes()) {
            if (p.id() == null || p.id().isBlank()) {
                throw new IllegalArgumentException("Every process needs an id");
            }
            if (!ids.add(p.id())) {
                throw new IllegalArgumentException("Duplicate process id: " + p.id());
            }
            if (p.arrivalTime() < 0) {
                throw new IllegalArgumentException("arrivalTime cannot be negative for " + p.id());
            }
            if (p.burstTime() <= 0) {
                throw new IllegalArgumentException("burstTime must be greater than 0 for " + p.id());
            }
        }
    }
}
