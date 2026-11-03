package com.devops.scheduler.service;

import com.devops.scheduler.model.GanttEntry;
import com.devops.scheduler.model.ProcessInput;
import com.devops.scheduler.model.ScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Round Robin: each process runs for at most one time quantum, then rejoins the queue. */
@Component
public class RoundRobinScheduler extends AbstractScheduler {

    @Override public String key() { return "round-robin"; }
    @Override public String displayName() { return "Round Robin"; }

    @Override
    public ScheduleResponse schedule(List<ProcessInput> processes, int timeQuantum) {
        List<ProcessInput> sorted = sortedByArrival(processes);
        Map<String, Integer> remaining = new HashMap<>();
        Map<String, Integer> completion = new HashMap<>();
        for (ProcessInput p : sorted) {
            remaining.put(p.id(), p.burstTime());
        }
        List<GanttEntry> gantt = new ArrayList<>();
        Deque<ProcessInput> queue = new ArrayDeque<>();
        int n = sorted.size();
        int next = 0;   // index of next process yet to arrive
        int done = 0;
        int time = 0;

        while (done < n) {
            while (next < n && sorted.get(next).arrivalTime() <= time) {
                queue.add(sorted.get(next++));
            }
            if (queue.isEmpty()) {   // idle until next arrival
                int arrival = sorted.get(next).arrivalTime();
                addSlice(gantt, IDLE, time, arrival);
                time = arrival;
                continue;
            }
            ProcessInput current = queue.poll();
            int run = Math.min(timeQuantum, remaining.get(current.id()));
            addSlice(gantt, current.id(), time, time + run);
            time += run;
            remaining.put(current.id(), remaining.get(current.id()) - run);

            // processes that arrived during this slice queue BEFORE the preempted one
            while (next < n && sorted.get(next).arrivalTime() <= time) {
                queue.add(sorted.get(next++));
            }
            if (remaining.get(current.id()) > 0) {
                queue.add(current);
            } else {
                completion.put(current.id(), time);
                done++;
            }
        }
        return buildResponse(processes, completion, gantt);
    }
}
