#!/usr/bin/env bash
# Deploys a release with Puppet. If the run fails or the application is not healthy afterwards, it goes back
# to the release that was marked stable before, and checks that one again.
#
#   ops/puppet/deploy.sh <release> [path-to-war]
#
# Exit code: 0 deployed and healthy; 3 the release failed and the previous stable release is running again;
#            1 or 4 the release failed and there is no healthy release to fall back to.
set -uo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
release="${1:?usage: deploy.sh <release> [path-to-war]}"
war="${2:-}"
stable_file=/opt/grievance-portal/stable
healthcheck=/usr/local/bin/gp-healthcheck

previous=""
[ -f "$stable_file" ] && previous="$(tr -d '[:space:]' < "$stable_file")"
echo "== Deploying release $release (stable release before this run: ${previous:-none})"

if env RELEASE="$release" ${war:+WAR_SOURCE="$war"} "$here/apply.sh" && "$healthcheck"; then
  echo "== Release $release is deployed, healthy and recorded as stable"
  exit 0
fi

echo "== Release $release FAILED: the Puppet run or the health check did not succeed"
if [ -z "$previous" ]; then
  echo "== There is no stable release to go back to"
  exit 1
fi
if [ "$previous" = "$release" ]; then
  echo "== The failed release is the stable one, nothing older to go back to"
  exit 1
fi

echo "== Rolling back to stable release $previous"
if env RELEASE="$previous" "$here/apply.sh" && "$healthcheck"; then
  echo "== Rolled back: release $previous is running and healthy again (release $release was rejected)"
  exit 3
fi
echo "== Rollback to $previous FAILED as well"
exit 4
