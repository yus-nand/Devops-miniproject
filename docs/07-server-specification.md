# Server specification for the Puppet-provisioned node

This page says what a server needs in order to run the Policyholder Grievance Portal. The Puppet code in `ops/puppet/` creates exactly this, and nothing else. The target is a clean Ubuntu 24.04 machine with systemd; the first run uses WSL2 Ubuntu on the development PC.

## What the server needs

| Kind | Item | Value | Why |
|---|---|---|---|
| Package | `openjdk-17-jre-headless` | installed | The application is compiled for Java 17. |
| Package | `tomcat10` | installed (Ubuntu 24.04 ships Tomcat 10.1) | The portal uses the Jakarta Servlet 6 API, which needs Tomcat 10.1 or newer. |
| Package | `curl` | installed | Used for the health check after a deployment. |
| User and group | `tomcat` | system account created by the `tomcat10` package | Runs the service; owns the data folder and the WAR. |
| Folder | `/var/lib/grievance-portal` | owner `tomcat:tomcat`, mode 0750 | Holds the H2 database files, so data lives outside the deployed application. |
| Folder | `/etc/systemd/system/tomcat10.service.d` | owner `root`, mode 0755 | Holds the override that keeps the data folder writable. |
| File | `/etc/default/tomcat10` | owner `root`, mode 0644 | `JAVA_HOME`, heap size, `GP_DB_PATH`, `GP_ENV_NAME`, security manager off. |
| File | `/etc/tomcat10/server.xml` | owner `root:tomcat`, mode 0640 | Minimal connector configuration: HTTP port, no shutdown port. |
| File | `/etc/systemd/system/tomcat10.service.d/grievance-portal.conf` | owner `root`, mode 0644 | `ReadWritePaths=/var/lib/grievance-portal`, because the packaged unit makes the rest of the file system read-only. |
| File | `/var/lib/tomcat10/webapps/grievance-portal.war` | owner `tomcat:tomcat`, mode 0640 | The release being deployed. Tomcat redeploys it by itself when the file changes. |
| Port | 8083/tcp | listening, HTTP | 8080 is the Windows Tomcat, 8081 Jenkins, 8082 the Docker container and 5000 the registry. The shutdown port is switched off. |
| Service | `tomcat10` | running, enabled at boot | Serves the application. |

Application settings that Puppet sets through `/etc/default/tomcat10`:

| Setting | Value | Effect |
|---|---|---|
| `GP_DB_PATH` | `/var/lib/grievance-portal/grievance-db` | H2 database file location. |
| `GP_ENV_NAME` | `puppet-node` | Shown in the page footer and in `/health`, so the node can be recognised. |

## What must exist before Puppet runs

These are the bootstrap steps that Puppet cannot do for itself:

1. Ubuntu 24.04 with systemd as the init system (`ps -p 1 -o comm=` prints `systemd`).
2. A user with `sudo`.
3. `puppet-agent` installed: `sudo apt-get update && sudo apt-get install -y puppet-agent` (Puppet 8.4 from the Ubuntu repositories).
4. The WAR built: `mvn package` writes `target/grievance-portal.war`.

## How the code is organised

| Path | Role |
|---|---|
| `ops/puppet/manifests/site.pp` | Entry point; hands the WAR location to the module. |
| `ops/puppet/modules/grievance_portal/manifests/init.pp` | The class with every resource above, in dependency order: package index, Java, Tomcat, user, folders, files, WAR, service. |
| `ops/puppet/modules/grievance_portal/templates/*.epp` | The three generated files. |
| `ops/puppet/apply.sh` | Checks the syntax, then runs `puppet apply` and prints whether anything changed. |

Class parameters (`http_port`, `env_name`, `data_dir`, heap sizes, Java package) have defaults in `init.pp`, so the specification above is the default configuration.

## How to run it

```bash
ops/puppet/apply.sh --noop     # shows what would change, changes nothing
ops/puppet/apply.sh            # applies it
curl -s http://localhost:8083/grievance-portal/health
```

A healthy result is `{"status":"UP","version":"1.0.0","environment":"puppet-node"}`. The first run installs and changes things, and `apply.sh` ends with "changes were applied". The same command run again ends with "no changes were needed", which is what the idempotency check in Task 14 relies on.

## Not covered here

Provisioning a node from scratch on demand, running the code a second time as proof of idempotency, the automatic health check and rolling back to an earlier WAR are the next task.
