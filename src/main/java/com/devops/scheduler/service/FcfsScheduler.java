package com.devops.scheduler.service;

import com.devops.scheduler.model.GanttEntry;
import com.devops.scheduler.model.ProcessInput;
import com.devops.scheduler.model.ScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** First Come, First Served: run processes in arrival order. */
@Component
public class FcfsScheduler extends AbstractScheduler {

    @Override public String key() { return "fcfs"; }
    @Override public String displayName() { return "FCFS"; }

    @Override
    public ScheduleResponse schedule(List<ProcessInput> processes, int timeQuantum) {
        Map<String, Integer> completion = new HashMap<>();
        List<GanttEntry> gantt = new ArrayList<>();
        int time = 0;
        for (ProcessInput p : sortedByArrival(processes)) {
            if (time < p.arrivalTime()) {
                addSlice(gantt, IDLE, time, p.arrivalTime());
                time = p.arrivalTime();
            }
            addSlice(gantt, p.id(), time, time + p.burstTime());
            time += p.burstTime();
            completion.put(p.id(), time);
        }
        return buildResponse(processes, completion, gantt);
    }
}
