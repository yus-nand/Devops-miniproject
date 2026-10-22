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
| Deploy | Copies the WAR into Tomcat's `webapps` folder |
| Verify | Polls `/health` until it reports `UP`, otherwise the build fails |

The `DEPLOY_ENV` parameter picks the target: `local` deploys `/grievance-portal`, `staging` deploys `/grievance-portal-staging`. Both use the same Tomcat and the same H2 database file, so deploying one removes the other.

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

## Documents

- `docs/01-problem-definition.md`: problem, stakeholders, objectives, MVP scope
- `docs/02-agile-plan.md`: user stories, backlog, sprint plan, Definition of Done, DevOps lifecycle
- `docs/03-requirements-architecture.md`: requirements, workflow, architecture, data model, URLs

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
