# Continuous deployment with Jenkins and Docker

This page describes what happens between a commit on `develop` and a new container answering on port 8082. It describes the pipeline as it ran in Jenkins build #12.

## Flow

```
commit on develop
   |  Jenkins polls the repository every 2 minutes (pollSCM 'H/2 * * * *')
   v
Checkout -> Docker check -> Build -> Package -> Test -> Image -> Push -> Run container -> Verify
```

| Stage | What it does | Stops the run when |
|---|---|---|
| Checkout | Fetches the commit and computes the image tag `b<build number>-<short commit id>` | the repository cannot be reached |
| Docker check | `docker version` | the Docker engine does not answer |
| Build | `mvn clean test`, 130 unit tests | any unit test fails |
| Package | Builds `grievance-portal.war` | the build fails |
| Test | Starts a throw-away Tomcat on port 8090 and runs the Selenium suite against it | any browser test fails |
| Image | `docker build` with the tag above | the build fails |
| Push | `docker push` to the registry in `REGISTRY` (default `localhost:5000`) | the registry is not reachable |
| Run container | Pulls that tag from the registry, removes the old container, starts the new one on port 8082 with the `grievance-data` volume | the pull or the start fails |
| Verify | Polls `/health` until it reports `"status":"UP"` | the application does not come up within about 90 seconds |

Because the stages run in this order, nothing is built into an image, pushed or started unless the unit tests and the browser tests have passed. The Test stage is the gate.

## What identifies a deployment

* The image tag, for example `localhost:5000/grievance-portal:b12-c11f71b`: build number first, commit id second. The same tag appears in the registry, in `docker ps` and in the last line of the Jenkins console.
* The `environment` field of `/health`, for example `docker-build-12`, set from the build number when the container is started.
* The data lives in the named volume `grievance-data`, so replacing the container does not lose grievances.

## Local registry

```powershell
docker run -d --name registry --restart unless-stopped -p 5000:5000 registry:2
```

The tags it holds can be listed at <http://localhost:5000/v2/grievance-portal/tags/list>. Old tags stay there, which is what makes a manual rollback to an earlier build possible.

## Machine-specific settings

The Jenkins service does not inherit the `PATH` of the logged-in user, so the `Jenkinsfile` adds the Maven and Docker folders itself (`MAVEN_BIN`, `DOCKER_BIN`). Docker Desktop is installed for one user, so the Jenkins service has to log on as that user to reach the engine.

## Not covered here

Going back automatically when the health check fails is part of the provisioning and rollback task, not of this pipeline.
