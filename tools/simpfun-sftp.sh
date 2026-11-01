#!/usr/bin/env bash
# Interactive/batch SFTP helper for Simpfun instance.
# Usage:
#   ./tools/simpfun-sftp.sh                 # open interactive session
#   ./tools/simpfun-sftp.sh ls plugins
#   ./tools/simpfun-sftp.sh put local.jar plugins/local.jar
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${SIMPFUN_ENV_FILE:-$ROOT/.secrets/simpfun.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE — copy .secrets/simpfun.env.example and fill SFTP credentials." >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

: "${SIMPFUN_SFTP_HOST:?}"
: "${SIMPFUN_SFTP_PORT:?}"
: "${SIMPFUN_SFTP_USER:?}"
: "${SIMPFUN_SFTP_PASS:?}"

if ! command -v expect >/dev/null 2>&1; then
  echo "Need 'expect' (or install sshpass and adapt this script)." >&2
  exit 1
fi

REMOTE_CMD="${*:-}"

expect <<EOF
set timeout 60
spawn sftp -P $SIMPFUN_SFTP_PORT -oStrictHostKeyChecking=accept-new ${SIMPFUN_SFTP_USER}@${SIMPFUN_SFTP_HOST}
expect {
  -re "(?i)password:" { send "${SIMPFUN_SFTP_PASS}\r" }
  timeout { puts stderr "SFTP connect timeout"; exit 1 }
}
expect {
  "sftp>" {}
  -re "(?i)permission denied" { puts stderr "SFTP auth failed"; exit 1 }
}
if {"$REMOTE_CMD" ne ""} {
  send "$REMOTE_CMD\r"
  expect "sftp>"
  send "bye\r"
  expect eof
} else {
  interact
}
EOF
