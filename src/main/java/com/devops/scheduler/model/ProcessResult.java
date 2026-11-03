package com.devops.scheduler.model;

public record ProcessResult(String id, int arrivalTime, int burstTime, Integer priority,
                            int completionTime, int turnaroundTime, int waitingTime) {
}
