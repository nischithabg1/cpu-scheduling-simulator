package com.devops.scheduler.model;

import java.util.List;

public record ScheduleResponse(String algorithm,
                               List<ProcessResult> processes,
                               List<GanttEntry> gantt,
                               List<String> executionOrder,
                               double averageWaitingTime,
                               double averageTurnaroundTime) {
}
