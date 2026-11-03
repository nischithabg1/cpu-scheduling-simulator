package com.devops.scheduler.service;

import com.devops.scheduler.model.GanttEntry;
import com.devops.scheduler.model.ProcessInput;
import com.devops.scheduler.model.ScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shortest Job First (non-preemptive): among arrived processes, pick the shortest burst. */
@Component
public class SjfScheduler extends AbstractScheduler {

    @Override public String key() { return "sjf"; }
    @Override public String displayName() { return "SJF"; }

    @Override
    public ScheduleResponse schedule(List<ProcessInput> processes, int timeQuantum) {
        List<ProcessInput> pending = sortedByArrival(processes);
        Map<String, Integer> completion = new HashMap<>();
        List<GanttEntry> gantt = new ArrayList<>();
        int time = 0;
        while (!pending.isEmpty()) {
            ProcessInput next = null;
            for (ProcessInput p : pending) {
                if (p.arrivalTime() <= time
                        && (next == null || p.burstTime() < next.burstTime())) {
                    next = p;   // strict '<' keeps earliest arrival on ties
                }
            }
            if (next == null) {   // CPU idle until the next arrival
                int nextArrival = pending.get(0).arrivalTime();
                addSlice(gantt, IDLE, time, nextArrival);
                time = nextArrival;
                continue;
            }
            addSlice(gantt, next.id(), time, time + next.burstTime());
            time += next.burstTime();
            completion.put(next.id(), time);
            pending.remove(next);
        }
        return buildResponse(processes, completion, gantt);
    }
}
