#!/usr/bin/env bash
# Provisions a bare Ubuntu 24.04 node from scratch: installs Puppet, then deploys a release with deploy.sh.
# Run it as root (for example: wsl -d <distro> -u root -- bash ops/puppet/provision.sh).
#
#   ops/puppet/provision.sh [release] [path-to-war]      defaults: 1.0.0 and target/grievance-portal.war
set -uo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
release="${1:-1.0.0}"
war="${2:-}"

if [ "$(id -u)" -ne 0 ]; then
  echo "Run this as root." >&2
  exit 1
fi
if [ "$(ps -p 1 -o comm=)" != "systemd" ]; then
  echo "This node does not run systemd as process 1, so Tomcat cannot run as a service here." >&2
  echo "On WSL, add [boot] systemd=true to /etc/wsl.conf and restart the distro." >&2
  exit 1
fi

echo "== Node: $(. /etc/os-release; echo "$PRETTY_NAME"), host $(hostname), $(date)"
if command -v puppet > /dev/null 2>&1; then
  echo "== Puppet is already installed: $(puppet --version)"
else
  echo "== Installing Puppet (the only step that cannot be done by Puppet itself)"
  apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y puppet-agent || exit 1
fi

exec "$here/deploy.sh" "$release" ${war:+"$war"}
