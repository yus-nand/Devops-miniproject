# Task 11: Docker Image and Container Lifecycle

Project: Puppet-Provisioned Policyholder Grievance Portal
Date: 23 October 2026
Docker version: 29.7.2 (Docker Desktop on Windows)

The full command log, with the real output of every command, is `docs/evidence/task11-docker-lifecycle.txt`. This document explains each stage of it.

## 1. The image

The `Dockerfile` in the repository root builds on the official `tomcat:10.1-jre17` image (Tomcat 10.1.61 and Java 17) and copies in one file, the WAR that Maven built:

| Line | Purpose |
| --- | --- |
| `FROM tomcat:${TOMCAT_TAG}` | The base image. `TOMCAT_TAG` defaults to `10.1-jre17`. |
| `ENV GP_ENV_NAME=docker` | The label shown in the page footer and in `/health`. |
| `COPY target/grievance-portal.war ...` | Puts the WAR in Tomcat's `webapps` folder, so the app is served at `/grievance-portal`. |
| `EXPOSE 8080` | Documents the port Tomcat listens on inside the container. |

A `.dockerignore` keeps the build context to that one file, so the build sends only 6 MB to Docker.

## 2. Lifecycle, stage by stage

| Stage | Command | What happened |
| --- | --- | --- |
| Build the WAR | `mvn -B -q clean package -DskipTests` | Produced `target/grievance-portal.war`. |
| Build the image | `docker build --progress=plain -t grievance-portal:1.0.0 .` | The base image layer came from the cache, one new layer holds the WAR. The image is tagged `grievance-portal:1.0.0`. |
| List images | `docker images grievance-portal` | One image, 477 MB on disk (131 MB of content), almost all of it the Tomcat and Java base. |
| Run | `docker run -d --name grievance-portal -p 8082:8080 -v grievance-data:/usr/local/tomcat/data grievance-portal:1.0.0` | Started detached, container name `grievance-portal`, host port 8082 mapped to Tomcat's 8080, volume `grievance-data` mounted where the H2 database lives. |
| Running containers | `docker ps --filter name=grievance-portal` | Status `Up`, ports `0.0.0.0:8082->8080/tcp`. |
| Logs | `docker logs grievance-portal --tail 15` | Tomcat 10.1.61 deployed the WAR in about 4.5 s and started the protocol handler on port 8080 ("Server startup in [4626] milliseconds"). |
| Use the app | browser, http://localhost:8082/grievance-portal/ | Login page, home page with the footer `Environment: docker`, and a grievance submitted by `asha` (reference GRV-2026-0006). |
| Stop | `docker stop grievance-portal` | The container went to `Exited (143)`. Code 143 is a normal stop: Docker sent SIGTERM and Tomcat shut down. |
| Start again | `docker start grievance-portal` | The same container (same ID `7940a8fbc3eb`) came back as `Up`. |
| Stop and remove | `docker stop`, then `docker rm grievance-portal` | `docker ps -a` no longer lists the container. |
| Data outlives the container | `docker volume ls --filter name=grievance-data` | The volume `grievance-data` is still there. |
| Run a new container | the same `docker run` command | A new container (ID `1e957483191f`) started on the same volume. After login, My grievances still listed GRV-2026-0006, so the data survived the removal. |
| Clean up | `docker rm -f grievance-portal` | Removed the running container in one step. The image and the volume stay for the next task. |

## 3. What this shows

- **Image and container are different things.** The image `grievance-portal:1.0.0` is a fixed, tagged package. Containers are throw-away copies started from it, and each gets its own ID.
- **Stop is not remove.** A stopped container keeps its identity and can be started again, but only `docker rm` takes it away.
- **Container storage is temporary.** Without the volume, the database would have been lost with the container. With the volume mounted at `/usr/local/tomcat/data`, a new container picks up where the old one stopped.
- **Port mapping makes the app reachable.** Port 8080 exists only inside the container. `-p 8082:8080` publishes it on the host, and 8082 was chosen because 8080 is the local Tomcat and 8081 is Jenkins.

## 4. Useful commands

```
docker build -t grievance-portal:1.0.0 .
docker run -d --name grievance-portal -p 8082:8080 -v grievance-data:/usr/local/tomcat/data grievance-portal:1.0.0
docker ps                      # running containers (add -a for stopped ones too)
docker logs -f grievance-portal
docker stop grievance-portal   # then: docker start grievance-portal
docker rm grievance-portal     # or docker rm -f to stop and remove in one step
docker volume rm grievance-data   # deletes the saved data
docker rmi grievance-portal:1.0.0 # deletes the image
```
