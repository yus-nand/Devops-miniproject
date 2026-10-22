# Policyholder Grievance Portal

A small Java web application for recording and tracking insurance grievances. Policyholders submit grievances, officers move them through a controlled status workflow, and admins see a summary dashboard. It is a DevOps mini project that takes the application from Git to Jenkins, Selenium, Docker and Puppet.

## Project status

The application is built in sprints. The backlog and plan are in `docs/02-agile-plan.md`.

Version 1.0.0 is the complete MVP: all 15 user stories (US-01 to US-15) are implemented and tested. Policyholders submit, edit and track grievances. Officers and admins list, search and filter every grievance, see Overdue flags and follow the status workflow. Admins close or reopen grievances and see a summary dashboard.

## Technology

| Layer | Choice |
| --- | --- |
| Language | Java 17 or newer |
| Web | Jakarta Servlet 6.0 and JSP on Apache Tomcat 10.1 |
| Build | Maven 3.9, packaged as a WAR |
| Database | H2 in file mode |
| Tests | JUnit 5, Selenium WebDriver |
| CI and CD | Jenkins |
| Containers | Docker |
| Configuration management | Puppet |

## Repository layout

```
.github/        issue and pull request templates
docs/           project documents
docs/evidence/  screenshots and logs for each task
ops/puppet/     Puppet code
src/main/java   application code
src/main/webapp JSP views and static files
src/test/java   unit and Selenium tests
pom.xml         Maven build
```

The Jenkinsfile and Dockerfile will be added at the repository root.

## Build and run locally

Prerequisites: JDK 17 or newer, Maven 3.9, Apache Tomcat 10.1.

```
mvn clean package
copy target\grievance-portal.war <tomcat>\webapps\
<tomcat>\bin\startup.bat
```

Then open http://localhost:8080/grievance-portal/ .

## Continuous integration

A Jenkins job named `grievance-portal-ci` builds the project automatically. It runs on the developer machine at http://localhost:8081 (Tomcat keeps port 8080).

| Setting | Value |
| --- | --- |
| Source | this repository, branch `develop` |
| Trigger | Poll SCM, `H/2 * * * *` (about every two minutes; a webhook needs a public address) |
| Build | `mvn clean package` with JDK 21 and Maven 3.9 |
| Result | `target/grievance-portal.war` is archived as the build artefact |

Current Jenkins releases need Java 21 or newer to run. The project still targets Java 17, which a JDK 21 builds without changes.

### Pipeline job

`Jenkinsfile` in the repository root defines a second, pipeline-style job (`grievance-portal-pipeline`, "Pipeline script from SCM", branch `develop`). It is run on demand with **Build with Parameters**.

| Stage | What it does |
| --- | --- |
| Checkout | Fetches the branch from GitHub |
| Build | `mvn clean test`, test results are published to Jenkins |
| Package | `mvn package -DskipTests`, the WAR is archived |
| Test | Starts a throw-away Tomcat on port 8090 with an in-memory database, runs the Selenium browser tests against it, publishes the report and any failure screenshots. A failing test stops the pipeline here, before Deploy |
| Deploy | Copies the WAR into Tomcat's `webapps` folder |
| Verify | Polls `/health` until it reports `UP`, otherwise the build fails |

The Test stage runs `mvn verify -Pci-ui -Dskip.unit=true`. The same command works on a developer machine (Brave is used there), and it never touches the real Tomcat on port 8080. On the Jenkins machine the tests use the Chromium browser they find in a system-wide install folder (Chrome or Edge), because the Jenkins service cannot see a per-user Brave.

The `DEPLOY_ENV` parameter picks the target: `local` deploys `/grievance-portal`, `staging` deploys `/grievance-portal-staging`. Both use the same Tomcat and the same H2 database file, so deploying one removes the other.

### Continuous deployment with Docker

With `DEPLOY_TARGET=docker` (the default) the pipeline continues after the Test stage like this:

| Stage | What it does |
| --- | --- |
| Docker check | Stops early if Jenkins cannot reach the Docker engine (runs before the long build) |
| Image | `docker build`, tagged `localhost:5000/grievance-portal:b<build number>-<commit>` |
| Push | Pushes the tag to the registry and prints the tags the registry now holds |
| Run container | Pulls the image back, removes the old container and starts the new one on port 8082 |
| Verify | Waits for `/health` to report `UP`; the footer and `/health` show `docker-build-<build number>` |

A commit on `develop` starts a run within about two minutes (Poll SCM), and the container is replaced only if the unit tests and the browser tests passed. The registry is a local container:

```
docker run -d --restart unless-stopped --name registry -p 5000:5000 -v registry-data:/var/lib/registry registry:2
```

The tags it holds are listed at http://localhost:5000/v2/grievance-portal/tags/list. `DEPLOY_TARGET=tomcat` keeps the earlier behaviour of copying the WAR into the local Tomcat.

## Browser tests

Selenium tests in `src/test/java/com/grievanceportal/ui` drive a real browser through the five main journeys (plan: `docs/04-test-plan.md`). They run against an application that is already deployed, so deploy first, then:

```
mvn test -Pui
```

Brave is used if it is installed, otherwise Chrome or Edge, without a window. Useful options:

| Option | Meaning |
| --- | --- |
| `-Dui.headless=false` | show the browser while the tests run |
| `-Dui.browser=chrome` | prefer `brave`, `chrome` or `edge` |
| `-Dui.browser.path="C:\path\to\brave.exe"` | use this browser program |
| `-Dui.driver.path="C:\path\to\chromedriver.exe"` | use this driver instead of the automatic download |
| `-Dui.baseUrl=http://localhost:8080/grievance-portal` | address of the deployed application |
| `-Dui.forceFail=true` | run the test that fails on purpose, to see the failure screenshot |

A failing test saves a screenshot in `target/screenshots`. An HTML report is made with `mvn -Pui test surefire-report:report-only` (`target/site/surefire-report.html`). The normal `mvn test`, and the Jenkins jobs, skip these tests.

## Docker image

The `Dockerfile` packages the WAR on the official Tomcat 10.1 image with Java 17. Build the WAR first, because the image only copies it:

```
mvn clean package -DskipTests
docker build -t grievance-portal:1.0.0 .
docker run -d --name grievance-portal -p 8082:8080 -v grievance-data:/usr/local/tomcat/data grievance-portal:1.0.0
```

Then open http://localhost:8082/grievance-portal/ (port 8080 is the local Tomcat and 8081 is Jenkins). The `grievance-data` volume keeps the database when the container is removed. The page footer says `Environment: docker`. The full command walk-through is in `docs/05-container-lifecycle.md`.

## Demo accounts and configuration

The first start creates these synthetic accounts and a few sample grievances. All of them share the password `demo123`. They are for local demos only.

| Username | Role |
| --- | --- |
| `asha`, `ravi` | Policyholder |
| `officer1` | Officer |
| `officer2` | Officer (a second officer, to try picking up a grievance someone else already took) |
| `admin1` | Admin |

Seeding is safe to repeat: restarting the application does not duplicate users or sample grievances.

Settings are read from system properties, then environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `GP_DB_PATH` | `${catalina.base}/data/grievance-db` under Tomcat, otherwise `./data/grievance-db` | Location of the H2 database file. A value starting with `mem:` gives an in-memory database, used by the tests. |
| `GP_ENV_NAME` | `local` | Environment label shown in the page footer |

`GET /health` returns `{"status":"UP","version":"...","environment":"..."}` and needs no login. It answers 503 with `"status":"DOWN"` when the database cannot be reached.

## Server provisioning with Puppet

`ops/puppet/` holds the Puppet code that turns a clean Ubuntu 24.04 machine into a portal server: Java 17, Tomcat 10, a `tomcat`-owned data folder, the generated configuration files and the deployed WAR, served on port 8083. The specification it implements is `docs/07-server-specification.md`.

```bash
sudo apt-get update && sudo apt-get install -y puppet-agent   # once per node
ops/puppet/apply.sh --noop                                     # preview
ops/puppet/apply.sh                                            # apply
curl -s http://localhost:8083/grievance-portal/health
```

## Documents

- `docs/01-problem-definition.md`: problem, stakeholders, objectives, MVP scope
- `docs/02-agile-plan.md`: user stories, backlog, sprint plan, Definition of Done, DevOps lifecycle
- `docs/03-requirements-architecture.md`: requirements, workflow, architecture, data model, URLs
- `docs/04-test-plan.md`: browser test journeys and how the tests run
- `docs/05-container-lifecycle.md`: the Docker container lifecycle, from the command log
- `docs/06-continuous-deployment.md`: the commit-to-container flow of the Jenkins pipeline
- `docs/07-server-specification.md`: what a server needs, as implemented by the Puppet code

## Branching strategy

| Branch | Purpose |
| --- | --- |
| `main` | Always release-ready. Receives merges from `develop` only, and each release is tagged `vX.Y.Z`. The initial skeleton was committed directly. |
| `develop` | Integration branch. Every feature and fix is merged here by pull request. |
| `feature/<short-name>` | One user story or task. Cut from `develop`. Example: `feature/submit-grievance`. |
| `bugfix/<short-name>` | A fix for something already on `develop`. Cut from `develop`. |

Branch names are lowercase with hyphens.

## Commit messages

Format: `type: short summary in the imperative`, 72 characters or fewer.

Types: `feat`, `fix`, `docs`, `build`, `ci`, `test`, `refactor`, `chore`.

Examples: `feat: add grievance submission form`, `fix: reject empty resolution note`.

## Pull requests

- Open the pull request against `develop`.
- Link the issue it closes, for example `Closes #5`.
- The build must pass and every review comment must be answered before merging.
- A card is Done only when the Definition of Done in `docs/02-agile-plan.md` is met.
