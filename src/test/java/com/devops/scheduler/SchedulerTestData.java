package com.devops.scheduler;

import com.devops.scheduler.model.ProcessInput;

import java.util.List;

final class SchedulerTestData {
    private SchedulerTestData() { }

    /** The sample from the assignment: P1(0,5) P2(1,3) P3(2,2). */
    static List<ProcessInput> sample() {
        return List.of(
                new ProcessInput("P1", 0, 5, 2),
                new ProcessInput("P2", 1, 3, 1),
                new ProcessInput("P3", 2, 2, 3));
    }
}
