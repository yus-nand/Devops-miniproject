# Policyholder Grievance Portal

A small Java web application for recording and tracking insurance grievances. Policyholders submit grievances, officers move them through a controlled status workflow, and admins see a summary dashboard. It is a DevOps mini project that takes the application from Git to Jenkins, Selenium, Docker and Puppet.

## Project status

The application is built in sprints. The backlog and plan are in `docs/02-agile-plan.md`.

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
