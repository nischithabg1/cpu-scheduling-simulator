package com.devops.scheduler.service;

import com.devops.scheduler.model.ProcessInput;
import com.devops.scheduler.model.ScheduleResponse;

import java.util.List;

public interface Scheduler {
    /** URL-friendly key, e.g. "fcfs". */
    String key();

    String displayName();

    ScheduleResponse schedule(List<ProcessInput> processes, int timeQuantum);
}
