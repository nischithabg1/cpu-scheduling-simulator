package com.devops.scheduler;

import com.devops.scheduler.model.ProcessInput;
import com.devops.scheduler.model.ProcessResult;
import com.devops.scheduler.model.ScheduleResponse;
import com.devops.scheduler.service.FcfsScheduler;
import com.devops.scheduler.service.PriorityScheduler;
import com.devops.scheduler.service.RoundRobinScheduler;
import com.devops.scheduler.service.SjfScheduler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchedulerAlgorithmsTest {

    private static ProcessResult find(ScheduleResponse r, String id) {
        return r.processes().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void fcfsCalculatesCompletionTurnaroundAndWaitingTimes() {
        ScheduleResponse r = new FcfsScheduler().schedule(SchedulerTestData.sample(), 0);

        assertEquals(List.of("P1", "P2", "P3"), r.executionOrder());
        assertEquals(5, find(r, "P1").completionTime());
        assertEquals(8, find(r, "P2").completionTime());
        assertEquals(10, find(r, "P3").completionTime());
        assertEquals(0, find(r, "P1").waitingTime());
        assertEquals(4, find(r, "P2").waitingTime());
        assertEquals(6, find(r, "P3").waitingTime());
        assertEquals(3.33, r.averageWaitingTime());
        assertEquals(6.67, r.averageTurnaroundTime());
    }

    @Test
    void fcfsInsertsIdleTimeWhenCpuHasNothingToRun() {
        List<ProcessInput> input = List.of(
                new ProcessInput("A", 0, 2, null),
                new ProcessInput("B", 5, 1, null));
        ScheduleResponse r = new FcfsScheduler().schedule(input, 0);

        assertEquals(3, r.gantt().size());
        assertEquals("IDLE", r.gantt().get(1).processId());
        assertEquals(6, find(r, "B").completionTime());
        assertEquals(0, find(r, "B").waitingTime());
    }

    @Test
    void sjfPicksShortestAvailableJob() {
        ScheduleResponse r = new SjfScheduler().schedule(SchedulerTestData.sample(), 0);

        // P1 runs first (only one arrived), then P3 (burst 2) before P2 (burst 3)
        assertEquals(List.of("P1", "P3", "P2"), r.executionOrder());
        assertEquals(7, find(r, "P3").completionTime());
        assertEquals(10, find(r, "P2").completionTime());
        assertEquals(3.0, r.averageWaitingTime());
    }

    @Test
    void roundRobinHandlesProcessesNeedingMultipleTimeSlices() {
        ScheduleResponse r = new RoundRobinScheduler().schedule(SchedulerTestData.sample(), 2);

        assertEquals(List.of("P1", "P2", "P3", "P1", "P2", "P1"), r.executionOrder());
        assertEquals(10, find(r, "P1").completionTime());
        assertEquals(9, find(r, "P2").completionTime());
        assertEquals(6, find(r, "P3").completionTime());
        assertEquals(4.0, r.averageWaitingTime());
        assertEquals(7.33, r.averageTurnaroundTime());
    }

    @Test
    void roundRobinWithLargeQuantumBehavesLikeFcfs() {
        ScheduleResponse rr = new RoundRobinScheduler().schedule(SchedulerTestData.sample(), 100);
        ScheduleResponse fcfs = new FcfsScheduler().schedule(SchedulerTestData.sample(), 0);

        assertEquals(fcfs.executionOrder(), rr.executionOrder());
        assertEquals(fcfs.averageWaitingTime(), rr.averageWaitingTime());
    }

    @Test
    void priorityUsesLowerNumberAsHigherPriority() {
        List<ProcessInput> input = List.of(
                new ProcessInput("P1", 0, 3, 2),
                new ProcessInput("P2", 0, 2, 1),
                new ProcessInput("P3", 0, 1, 3));
        ScheduleResponse r = new PriorityScheduler().schedule(input, 0);

        assertEquals(List.of("P2", "P1", "P3"), r.executionOrder());
    }
}
