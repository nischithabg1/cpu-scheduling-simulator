package com.devops.scheduler.model;

/** One process submitted by the user. Lower priority number = higher priority. */
public record ProcessInput(String id, int arrivalTime, int burstTime, Integer priority) {
    public int priorityOrDefault() {
        return priority == null ? 0 : priority;
    }
}
