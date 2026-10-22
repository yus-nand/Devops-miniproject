#!/usr/bin/env bash
# Makes a deliberately faulty release for the rollback demonstration: the real WAR without its database driver,
# so the application cannot start and /health never reports UP.
#
#   ops/puppet/make-broken-war.sh <good.war> <broken.war>
set -euo pipefail

in="${1:?usage: make-broken-war.sh <good.war> <broken.war>}"
out="${2:?usage: make-broken-war.sh <good.war> <broken.war>}"

python3 - "$in" "$out" <<'PY'
import sys, zipfile

src, dst = sys.argv[1], sys.argv[2]
removed = []
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        if item.filename.startswith("WEB-INF/lib/h2-") and item.filename.endswith(".jar"):
            removed.append(item.filename)
            continue
        zout.writestr(item, zin.read(item.filename))
if not removed:
    sys.exit("no H2 driver found in " + src)
print("wrote " + dst + ", removed " + ", ".join(removed))
PY
