#!/usr/bin/env bash
# Applies the Puppet code in this folder to the machine it runs on (masterless).
#
#   ops/puppet/apply.sh            check the syntax, then apply
#   ops/puppet/apply.sh --noop     check the syntax, then show what would change, changing nothing
#
# The WAR to deploy comes from WAR_SOURCE (default: target/grievance-portal.war in this repository).
set -uo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
export FACTER_war_source="${WAR_SOURCE:-$repo/target/grievance-portal.war}"

if ! command -v puppet > /dev/null 2>&1; then
  echo "puppet is not installed. Run: sudo apt-get update && sudo apt-get install -y puppet-agent" >&2
  exit 1
fi
if [ ! -f "$FACTER_war_source" ]; then
  echo "WAR not found: $FACTER_war_source (build it with mvn package, or set WAR_SOURCE)" >&2
  exit 1
fi

echo "== Puppet $(puppet --version), WAR $FACTER_war_source"
echo "== Syntax check"
puppet parser validate "$here/manifests/site.pp" "$here/modules/grievance_portal/manifests/init.pp" || exit 1
puppet epp validate "$here"/modules/grievance_portal/templates/*.epp || exit 1
echo "syntax ok"

echo "== Apply${*:+ ($*)}"
sudo --preserve-env=FACTER_war_source puppet apply --detailed-exitcodes \
  --modulepath "$here/modules" "$here/manifests/site.pp" "$@"
code=$?
case $code in
  0) echo "== Result: no changes were needed" ;;
  2) echo "== Result: changes were applied (or, with --noop, would be applied)" ;;
  *) echo "== Result: FAILED (exit code $code)" ;;
esac
[ "$code" -eq 0 ] || [ "$code" -eq 2 ]
