
const algorithmSelect = document.getElementById("algorithm");
const processList = document.getElementById("process-list");
const quantumGroup = document.getElementById("quantum-group");
const errorMessage = document.getElementById("error-message");
const calculateButton = document.getElementById("calculate-btn");
const resultsSection = document.getElementById("results");

const colors = [
    "#5267e8", "#13a58b", "#e49b36",
    "#9a65d6", "#e16b83", "#328bb8",
    "#718b49", "#b16c49"
];

function updateQuantumVisibility() {
    quantumGroup.classList.toggle(
        "hidden",
        algorithmSelect.value !== "round-robin"
    );
}

algorithmSelect.addEventListener("change", updateQuantumVisibility);

function addProcess(id = "", arrival = 0, burst = 1, priority = 1) {
    const row = document.createElement("tr");

    row.innerHTML = `
        <td><input class="pid" aria-label="Process ID"
            value="${id}" placeholder="P4"></td>
        <td><input class="arrival" type="number" min="0"
            value="${arrival}" aria-label="Arrival time"></td>
        <td><input class="burst" type="number" min="1"
            value="${burst}" aria-label="Burst time"></td>
        <td><input class="priority" type="number"
            value="${priority}" aria-label="Priority"></td>
        <td><button class="remove-btn" type="button"
            aria-label="Remove process">×</button></td>
    `;

    processList.appendChild(row);
}

document.getElementById("add-process").addEventListener("click", () => {
    addProcess(`P${processList.children.length + 1}`);
});

processList.addEventListener("click", event => {
    if (event.target.classList.contains("remove-btn")) {
        if (processList.children.length > 1) {
            event.target.closest("tr").remove();
        } else {
            showError("At least one process is required.");
        }
    }
});

function showError(message) {
    errorMessage.textContent = message;
}

function getProcesses() {
    const rows = [...processList.querySelectorAll("tr")];

    const processes = rows.map(row => ({
        id: row.querySelector(".pid").value.trim(),
        arrivalTime: Number(row.querySelector(".arrival").value),
        burstTime: Number(row.querySelector(".burst").value),
        priority: Number(row.querySelector(".priority").value)
    }));

    if (processes.some(p => !p.id)) {
        throw new Error("Every process must have an ID.");
    }

    if (new Set(processes.map(p => p.id)).size !== processes.length) {
        throw new Error("Process IDs must be unique.");
    }

    if (processes.some(p =>
        !Number.isInteger(p.arrivalTime) ||
        p.arrivalTime < 0 ||
        !Number.isInteger(p.burstTime) ||
        p.burstTime <= 0 ||
        !Number.isInteger(p.priority)
    )) {
        throw new Error(
            "Arrival time must be non-negative, burst time must be positive, and all values must be integers."
        );
    }

    return processes;
}

function renderGantt(gantt) {
    const chart = document.getElementById("gantt-chart");
    const labels = document.getElementById("time-labels");

    chart.innerHTML = "";
    labels.innerHTML = "";

    if (!gantt || gantt.length === 0) {
        chart.textContent = "No execution data available.";
        return;
    }

    const colorMap = new Map();
    let colorIndex = 0;

    const getColor = id => {
        if (!colorMap.has(id)) {
            colorMap.set(id, colors[colorIndex % colors.length]);
            colorIndex++;
        }
        return colorMap.get(id);
    };

    const firstTime = Math.min(...gantt.map(g => g.start));
    const lastTime = Math.max(...gantt.map(g => g.end));
    const totalDuration = lastTime - firstTime;

    if (totalDuration <= 0) return;

    let previousEnd = firstTime;

    gantt.forEach(entry => {
        if (entry.start > previousEnd) {
            const idle = document.createElement("div");
            idle.className = "gantt-block";
            idle.style.flex = entry.start - previousEnd;
            idle.style.background = "#9ca3af";
            idle.innerHTML = `Idle<small>${previousEnd}–${entry.start}</small>`;
            chart.appendChild(idle);
        }

        const duration = entry.end - entry.start;
        const block = document.createElement("div");

        block.className = "gantt-block";
        block.style.flex = duration;
        block.style.background = getColor(entry.processId);
        block.innerHTML = `
            ${entry.processId}
            <small>${entry.start}–${entry.end}</small>
        `;

        block.title = `${entry.processId}: ${entry.start} to ${entry.end}`;
        chart.appendChild(block);

        previousEnd = entry.end;
    });

    labels.innerHTML = `
        <span>Start: ${firstTime}</span>
        <span>End: ${lastTime}</span>
    `;
}

function renderResults(data) {
    document.getElementById("result-algorithm").textContent =
        `Algorithm: ${data.algorithm}`;

    document.getElementById("avg-wait").textContent =
        Number(data.averageWaitingTime).toFixed(2);

    document.getElementById("avg-turnaround").textContent =
        Number(data.averageTurnaroundTime).toFixed(2);

    document.getElementById("total-processes").textContent =
        data.processes.length;

    const totalTime = data.gantt.length
        ? Math.max(...data.gantt.map(g => g.end))
        : 0;

    document.getElementById("total-time").textContent = totalTime;

    document.getElementById("execution-order").textContent =
        "Execution Order: " + data.executionOrder.join(" → ");

    renderGantt(data.gantt);

    const table = document.getElementById("result-table");
    table.innerHTML = "";

    data.processes.forEach(process => {
        const row = document.createElement("tr");

        [
            process.id,
            process.arrivalTime,
            process.burstTime,
            process.completionTime,
            process.turnaroundTime,
            process.waitingTime
        ].forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        });

        table.appendChild(row);
    });

    resultsSection.classList.remove("hidden");
    resultsSection.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

calculateButton.addEventListener("click", async () => {
    showError("");

    try {
        const algorithm = algorithmSelect.value;
        const processes = getProcesses();

        const requestBody = {
            processes: processes,
            timeQuantum: algorithm === "round-robin"
                ? Number(document.getElementById("quantum").value)
                : null
        };

        if (
            algorithm === "round-robin" &&
            (!Number.isInteger(requestBody.timeQuantum) ||
             requestBody.timeQuantum <= 0)
        ) {
            throw new Error("Time quantum must be a positive integer.");
        }

        calculateButton.disabled = true;
        calculateButton.textContent = "Calculating...";

        const response = await fetch(`/schedule/${algorithm}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(requestBody)
        });

        if (!response.ok) {
            const error = await response.json().catch(() => null);
            throw new Error(
                error?.message || `Server error: HTTP ${response.status}`
            );
        }

        const data = await response.json();
        renderResults(data);

    } catch (error) {
        showError(error.message || "Unable to calculate the schedule.");
    } finally {
        calculateButton.disabled = false;
        calculateButton.textContent = "Calculate Schedule →";
    }
});

document.getElementById("reset-btn").addEventListener("click", () => {
    processList.innerHTML = "";

    addProcess("P1", 0, 5, 2);
    addProcess("P2", 1, 3, 1);
    addProcess("P3", 2, 2, 3);

    algorithmSelect.value = "fcfs";
    document.getElementById("quantum").value = 2;

    updateQuantumVisibility();
    showError("");
    resultsSection.classList.add("hidden");
});

updateQuantumVisibility();
