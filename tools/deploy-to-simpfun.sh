#!/usr/bin/env bash
# Upload a plugin jar to Simpfun plugins/ via SFTP.
# Usage:
#   ./tools/deploy-to-simpfun.sh path/to/plugin.jar
#   ./tools/deploy-to-simpfun.sh path/to/plugin.jar MyAntiAim.jar
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${SIMPFUN_ENV_FILE:-$ROOT/.secrets/simpfun.env}"

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <local-jar> [remote-filename]" >&2
  exit 1
fi

LOCAL_JAR="$1"
if [[ ! -f "$LOCAL_JAR" ]]; then
  echo "File not found: $LOCAL_JAR" >&2
  exit 1
fi

REMOTE_NAME="${2:-$(basename "$LOCAL_JAR")}"
REMOTE_PATH="plugins/${REMOTE_NAME}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE — copy .secrets/simpfun.env.example first." >&2
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
  echo "Need 'expect'." >&2
  exit 1
fi

ABS_JAR="$(cd "$(dirname "$LOCAL_JAR")" && pwd)/$(basename "$LOCAL_JAR")"

echo "Uploading $ABS_JAR -> ${SIMPFUN_SFTP_USER}@${SIMPFUN_SFTP_HOST}:${REMOTE_PATH}"

expect <<EOF
set timeout 120
spawn sftp -P $SIMPFUN_SFTP_PORT -oStrictHostKeyChecking=accept-new ${SIMPFUN_SFTP_USER}@${SIMPFUN_SFTP_HOST}
expect {
  -re "(?i)password:" { send "${SIMPFUN_SFTP_PASS}\r" }
  timeout { puts stderr "SFTP connect timeout"; exit 1 }
}
expect "sftp>"
send "mkdir plugins\r"
expect "sftp>"
send "put ${ABS_JAR} ${REMOTE_PATH}\r"
expect {
  "sftp>" {}
  timeout { puts stderr "upload timeout"; exit 1 }
}
send "ls -l plugins\r"
expect "sftp>"
send "bye\r"
expect eof
EOF

echo "Done. Next: open ${SIMPFUN_PANEL_URL:-panel} → 重启实例, then verify in 终端 logs."
echo "Game join: ${SIMPFUN_GAME_HOST:-play.simpfun.cn}:${SIMPFUN_GAME_PORT:-17184}"
