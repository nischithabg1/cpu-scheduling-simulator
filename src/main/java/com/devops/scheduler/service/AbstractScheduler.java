package com.devops.scheduler.service;

import com.devops.scheduler.model.GanttEntry;
import com.devops.scheduler.model.ProcessInput;
import com.devops.scheduler.model.ProcessResult;
import com.devops.scheduler.model.ScheduleResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Shared helpers: Gantt-chart building and metric calculation. */
abstract class AbstractScheduler implements Scheduler {

    static final String IDLE = "IDLE";

    /** Processes sorted by arrival time (stable, so ties keep input order). */
    protected List<ProcessInput> sortedByArrival(List<ProcessInput> processes) {
        List<ProcessInput> sorted = new ArrayList<>(processes);
        sorted.sort(Comparator.comparingInt(ProcessInput::arrivalTime));
        return sorted;
    }

    /** Appends a block, merging it with the previous one if the same process continues. */
    protected void addSlice(List<GanttEntry> gantt, String id, int start, int end) {
        if (end <= start) {
            return;
        }
        if (!gantt.isEmpty()) {
            GanttEntry last = gantt.get(gantt.size() - 1);
            if (last.processId().equals(id) && last.end() == start) {
                gantt.set(gantt.size() - 1, new GanttEntry(id, last.start(), end));
                return;
            }
        }
        gantt.add(new GanttEntry(id, start, end));
    }

    /**
     * Turnaround Time = Completion Time - Arrival Time
     * Waiting Time    = Turnaround Time - Burst Time
     */
    protected ScheduleResponse buildResponse(List<ProcessInput> original,
                                             Map<String, Integer> completion,
                                             List<GanttEntry> gantt) {
        List<ProcessResult> results = new ArrayList<>();
        double totalWaiting = 0;
        double totalTurnaround = 0;
        for (ProcessInput p : original) {
            int ct = completion.get(p.id());
            int tat = ct - p.arrivalTime();
            int wt = tat - p.burstTime();
            totalWaiting += wt;
            totalTurnaround += tat;
            results.add(new ProcessResult(p.id(), p.arrivalTime(), p.burstTime(), p.priority(), ct, tat, wt));
        }
        List<String> order = gantt.stream()
                .map(GanttEntry::processId)
                .filter(id -> !IDLE.equals(id))
                .toList();
        int n = original.size();
        return new ScheduleResponse(displayName(), results, gantt, order,
                round2(totalWaiting / n), round2(totalTurnaround / n));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
