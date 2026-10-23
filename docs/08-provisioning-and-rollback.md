# Provisioning, idempotency, health check and rollback

This page describes how the Puppet-provisioned node is built from nothing, shown to be repeatable, checked for health, and recovered when a release is bad. Every figure below is taken from the logs in `docs/evidence/`, which were produced on a freshly created WSL2 Ubuntu 24.04 node (`grievance-node`) that had no Puppet, Java, Tomcat or `tomcat` user.

## How the pieces fit

| Script | Does |
|---|---|
| `ops/puppet/provision.sh [release] [war]` | On a bare node: checks that systemd is process 1, installs `puppet-agent` (the one step Puppet cannot do for itself), then calls `deploy.sh`. |
| `ops/puppet/deploy.sh <release> [war]` | Runs `apply.sh`, then the health check. If either fails, it re-applies the release that was marked stable before and checks that one too. |
| `ops/puppet/apply.sh` | Checks the syntax and runs `puppet apply` with the chosen release. |
| `ops/puppet/make-broken-war.sh` | Builds a deliberately faulty release (the real WAR without its H2 database driver) for the rollback demonstration. |

Inside the `grievance_portal` class:

* **Releases.** Every WAR is stored once under `/opt/grievance-portal/releases/<release>/` and never replaced. The deployed WAR is a copy of the chosen release.
* **Restart on change.** A changed WAR removes the old unpacked application and restarts Tomcat (auto-deploy is off), so the health check always judges the new release.
* **Health check.** `/usr/local/bin/gp-healthcheck`, installed by Puppet, asks `/health` up to 20 times, three seconds apart, and succeeds only on `"status":"UP"`. A Puppet `exec` runs it after every WAR change or restart; if it fails, the Puppet run fails.
* **Stable marker.** `/opt/grievance-portal/stable` names the last release that passed its health check. It is written only when the release is not recorded yet and the application is healthy, so a failed release is never recorded and a repeated run writes nothing.

## 1. Clean node, first run, second run

| | First run | Second run |
|---|---|---|
| Command | `provision.sh 1.0.0` | `provision.sh 1.0.0` |
| Node before | no `puppet`, `java`, `tomcat10`; `/opt` empty; no `tomcat` user | provisioned by the first run |
| Puppet 8.4 | installed by the script | already installed |
| Resources changed | 18 (2 packages, 11 files and folders, 4 execs, the service) | 0 |
| Puppet run time | 49.42 s | 0.30 s |
| Result line | "changes were applied" | "no changes were needed" |
| Health check | passed on attempt 2 (inside Puppet), then on attempt 1 | passed on attempt 1 |
| Stable release | none, then 1.0.0 | 1.0.0 |
| Exit code | 0 | 0 |

Logs: `docs/evidence/task14-1-provision-from-scratch.txt` and `docs/evidence/task14-2-second-run.txt`. The second run changing nothing is the idempotency result: the health check and the stable marker are written so that they act only when something is different.

The same behaviour was seen earlier on the Task 13 node when the release layout was added to it: one run applied the changes and marked 1.0.0 stable, the next changed nothing.

## 2. Rollback

`deploy.sh 1.0.1-broken target/grievance-portal-broken.war` on the node that runs 1.0.0 (stable):

| Step | What happened |
|---|---|
| Release stored and deployed | `releases/1.0.1-broken/` created, WAR copied, old unpacked app removed, Tomcat restarted |
| Health check | failed after 20 attempts (HTTP 404, the application cannot start without its database driver); the Puppet run took 57.96 s and exited 6 |
| Stable marker | `mark-stable` skipped because its dependency failed; `stable` stayed `1.0.0` |
| Rollback | `deploy.sh` applied release 1.0.0 from the copy already on the node; WAR replaced, Tomcat restarted, healthy on attempt 2 (4.64 s) |
| Re-check | health check passed again on attempt 1 |
| End state | exit code 3 ("release rejected, previous stable release running"); `/health` answers `{"status":"UP","version":"1.0.0","environment":"puppet-node"}`; `stable` is `1.0.0`; both releases remain stored |

Log: `docs/evidence/task14-3-rollback.txt`.

Exit codes of `deploy.sh`: 0 deployed and healthy; 3 failed and rolled back; 1 or 4 failed with nothing to go back to.

## What this does not claim

* One node. The second node of a real fleet would need Puppet to be told about it, which is outside this project.
* The faulty release is built on purpose. It shows that a release which cannot start is detected and replaced, not that every possible fault is.
* Releases are chosen by name by whoever runs `deploy.sh`; the Jenkins pipeline (Task 12) deploys the Docker image and is not connected to this node.
* A WSL distribution can stop when no session is open, so the node is only guaranteed to be reachable while one is. The demonstration ran with a session open.
