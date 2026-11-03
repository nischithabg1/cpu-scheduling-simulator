package com.devops.scheduler.model;

/** A block on the Gantt chart. processId is "IDLE" when the CPU has nothing to run. */
public record GanttEntry(String processId, int start, int end) {
}
