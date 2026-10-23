#!/usr/bin/env bash
# Applies the Puppet code in this folder to the machine it runs on (masterless).
#
#   ops/puppet/apply.sh            check the syntax, then apply
#   ops/puppet/apply.sh --noop     check the syntax, then show what would change, changing nothing
#
# RELEASE     release to run (default 1.0.0)
# WAR_SOURCE  WAR to store as that release. Default: the copy the node already holds for that release, or
#             target/grievance-portal.war in this repository when the node does not hold it yet.
set -uo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
release="${RELEASE:-1.0.0}"
stored="/opt/grievance-portal/releases/$release/grievance-portal.war"

unset FACTER_war_source
export FACTER_gp_release="$release"
if [ -n "${WAR_SOURCE:-}" ]; then
  # Puppet only accepts absolute paths, so a path relative to the current folder is resolved here.
  export FACTER_war_source="$(readlink -f "$WAR_SOURCE")"
elif [ ! -f "$stored" ]; then
  export FACTER_war_source="$repo/target/grievance-portal.war"
fi

noop=0
for arg in "$@"; do
  [ "$arg" = "--noop" ] && noop=1
done

if ! command -v puppet > /dev/null 2>&1; then
  echo "puppet is not installed. Run: sudo apt-get update && sudo apt-get install -y puppet-agent" >&2
  exit 1
fi
if [ -n "${FACTER_war_source:-}" ] && [ ! -f "$FACTER_war_source" ]; then
  echo "WAR not found: $FACTER_war_source (build it with mvn package, or set WAR_SOURCE)" >&2
  exit 1
fi

echo "== Puppet $(puppet --version), release $release, WAR ${FACTER_war_source:-already stored on this node}"
echo "== Syntax check"
puppet parser validate "$here/manifests/site.pp" "$here/modules/grievance_portal/manifests/init.pp" || exit 1
puppet epp validate "$here"/modules/grievance_portal/templates/*.epp || exit 1
echo "syntax ok"

echo "== Apply${*:+ ($*)}"
sudo --preserve-env=FACTER_war_source,FACTER_gp_release puppet apply --detailed-exitcodes \
  --modulepath "$here/modules" "$here/manifests/site.pp" "$@"
code=$?
case $code in
  0) if [ "$noop" -eq 1 ]; then
       # With --noop Puppet exits 0 even when it plans changes, so the Notice lines above are the answer.
       echo "== Result: dry run finished, nothing was changed (the Notice lines above show what a real run would change)"
     else
       echo "== Result: no changes were needed"
     fi ;;
  2) echo "== Result: changes were applied" ;;
  *) echo "== Result: FAILED (exit code $code)" ;;
esac
[ "$code" -eq 0 ] || [ "$code" -eq 2 ]
