package com.devops.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SchedulerApiTest {

    @Autowired
    private MockMvc mvc;

    private static final String BODY = """
            {"timeQuantum": 2, "processes": [
              {"id":"P1","arrivalTime":0,"burstTime":5},
              {"id":"P2","arrivalTime":1,"burstTime":3},
              {"id":"P3","arrivalTime":2,"burstTime":2}]}
            """;

    @Test
    void healthEndpointReportsUp() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void algorithmsEndpointListsAllAlgorithms() throws Exception {
        mvc.perform(get("/algorithms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void fcfsEndpointReturnsCalculatedResults() throws Exception {
        mvc.perform(post("/schedule/fcfs").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("FCFS"))
                .andExpect(jsonPath("$.averageWaitingTime").value(3.33));
    }

    @Test
    void roundRobinEndpointUsesTimeQuantum() throws Exception {
        mvc.perform(post("/schedule/round-robin").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageWaitingTime").value(4.0));
    }

    @Test
    void invalidInputReturns400() throws Exception {
        mvc.perform(post("/schedule/fcfs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processes\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/schedule/unknown").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
    }
}
