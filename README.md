# CPU Scheduling Simulator (DevOps Assignment)

Spring Boot REST API that simulates **FCFS, SJF, Round Robin and Priority** scheduling, taken through
a DevOps pipeline: **Git → Maven → Jenkins → Ansible → Docker → Kubernetes**.

| Component | Tools | Enhancement |
|---|---|---|
| 1 | Git/GitHub, Maven, Jenkins | Automated unit tests in the Jenkins pipeline |
| 2 | Ansible, Docker, Kubernetes | Liveness + readiness probes on `/health` |

## API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/algorithms` | List algorithms (`fcfs`, `priority`, `round-robin`, `sjf`) |
| POST | `/schedule/{algorithm}` | Run an algorithm on the submitted processes |
| GET | `/health` | Health check used by Kubernetes probes |

Turnaround = Completion − Arrival; Waiting = Turnaround − Burst. Priority: lower number = higher priority.

```bash
curl -X POST http://localhost:8080/schedule/fcfs -H "Content-Type: application/json" -d '{
  "timeQuantum": 2,
  "processes": [
    {"id":"P1","arrivalTime":0,"burstTime":5,"priority":2},
    {"id":"P2","arrivalTime":1,"burstTime":3,"priority":1},
    {"id":"P3","arrivalTime":2,"burstTime":2,"priority":3}]}'
```
Expected FCFS: order P1→P2→P3, avg waiting 3.33, avg turnaround 6.67.
Use `/schedule/round-robin` (uses `timeQuantum`) for Round Robin: avg waiting 4.0, avg turnaround 7.33.

---
## Component 1 - Git, Maven, Jenkins

### 1. Build and test locally
```bash
mvn clean package          # runs unit tests, creates target/cpu-scheduling-simulator.jar
java -jar target/cpu-scheduling-simulator.jar
```

### 2. Git: two branches and a merge (screenshot this)
Create an empty repo `cpu-scheduling-simulator` on GitHub first, then:
```bash
git init -b main
git add .
git commit -m "Initial commit: CPU scheduling simulator"
git remote add origin https://github.com/<you>/cpu-scheduling-simulator.git
git push -u origin main

git checkout -b feature/priority-scheduling
# make a real change, e.g. tweak README or add a test case
git add . && git commit -m "Add feature: priority scheduling improvements"
git push -u origin feature/priority-scheduling
# On GitHub: open a Pull Request -> merge into main  (or: git checkout main && git merge --no-ff feature/priority-scheduling)
git checkout main && git pull
git log --oneline --graph --all        # screenshot
```
Tip: to make the history genuine, commit SJF/Priority on feature branches *before* merging them into main
(e.g. delete `SjfScheduler` from the first commit, then add it on `feature/sjf-scheduling`).

### 3. Jenkins
1. Install Jenkins (needs Java 17+) plus JDK 17 and Maven on the Jenkins machine; plugins: Git, Pipeline, GitHub, JUnit.
2. New Item -> **Pipeline** -> *Pipeline script from SCM* -> Git -> your repo URL -> branch `*/main` -> Script Path `Jenkinsfile`.
3. Build Triggers: tick **GitHub hook trigger for GITScm polling** (webhook: `http://<jenkins-url>/github-webhook/`) or **Poll SCM** `H/2 * * * *`.
   For a local Jenkins, polling is easier than a webhook (GitHub can't reach localhost).
4. Push a commit -> a new build starts automatically.

Pipeline stages: **Git Checkout -> Test -> Build -> Archive Artifact.**
Screenshots: job config, successful stage view, *Test Result* page, archived `cpu-scheduling-simulator.jar`.
Bonus screenshot: break a test on purpose, push, show the failed build, then fix it.

---
## Component 2 - Ansible, Docker, Kubernetes

### 1. Ansible (run `mvn clean package` first so the JAR exists)
```bash
sudo apt install ansible          # Ubuntu / WSL2 with systemd
cd ansible
ansible-playbook playbook.yml     # or: ansible-playbook playbook.yml -e app_port=9090
curl http://localhost:8080/health
```
Requirements covered: inventory (`inventory`), playbook (`playbook.yml`), variable (`app_port`),
handlers (`Restart application`, `Reload systemd`), role (`roles/cpu_scheduler`).
For your report, mention the role skeleton is created with `ansible-galaxy init cpu_scheduler`
(run it in a scratch folder and screenshot it - the finished role is already in this repo).
Note: the role uses systemd. If you're on Windows without WSL2+systemd, run Ansible from a Linux VM,
or screenshot a `--check` run on a Linux machine.

### 2. Docker
```bash
docker build -t cpu-scheduling-simulator:1.0 .
docker run -d -p 8080:8080 --name sim cpu-scheduling-simulator:1.0
curl http://localhost:8080/algorithms
docker stop sim && docker rm sim
```
(If Ansible is already using port 8080, stop it first: `sudo systemctl stop cpu-scheduler`, or map `-p 8081:8080`.)

### 3. Kubernetes (Minikube shown; Kind also works)
```bash
minikube start
minikube image load cpu-scheduling-simulator:1.0      # kind: kind load docker-image cpu-scheduling-simulator:1.0
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl get pods            # 3 pods Running (1/1 READY)
kubectl get svc
minikube service cpu-scheduling-simulator-service --url   # then curl <url>/algorithms
```

### 4. Scaling (screenshot each `get pods`)
```bash
kubectl scale deployment cpu-scheduling-simulator --replicas=5
kubectl get pods
kubectl scale deployment cpu-scheduling-simulator --replicas=3
kubectl get pods
```

### 5. Enhancement: liveness and readiness probes
Both probes call `GET /health` (see `kubernetes/deployment.yaml`). Evidence:
```bash
kubectl describe pod <pod-name>     # shows Liveness / Readiness lines and no probe-failure events
kubectl get pods                    # READY 1/1 only after the readiness probe passes
```

---
## Project layout
```
src/main      Spring Boot app + scheduling algorithms
src/test      Unit tests (algorithms) + API tests
pom.xml       Maven build -> target/cpu-scheduling-simulator.jar
Jenkinsfile   CI pipeline
Dockerfile    Container image
ansible/      inventory, playbook, roles/cpu_scheduler (tasks, handlers, defaults, templates)
kubernetes/   deployment.yaml (3 replicas + probes), service.yaml
repo_link.txt GitHub repository URL
```
