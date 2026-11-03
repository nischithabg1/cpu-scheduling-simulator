package com.devops.scheduler.model;

import java.util.List;

public record ScheduleRequest(List<ProcessInput> processes, Integer timeQuantum) {
}
