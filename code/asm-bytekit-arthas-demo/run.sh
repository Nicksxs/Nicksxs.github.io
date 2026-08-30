#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
MODE="${1:-baseline}"
ITERATIONS="${2:-5}"
PAUSE_MILLIS="${3:-300}"
MVN_BIN="${MVN_BIN:-mvn}"
JAVA_BIN="${JAVA_BIN:-java}"

if [[ ! -d "$ROOT_DIR/target/classes" ]]; then
  if ! command -v "$MVN_BIN" >/dev/null 2>&1; then
    echo "mvn was not found. Install Maven or set MVN_BIN to its absolute path." >&2
    exit 1
  fi
  "$MVN_BIN" -q -f "$ROOT_DIR/pom.xml" package
fi

CLASSPATH="$ROOT_DIR/target/classes:$ROOT_DIR/target/dependency/*"
MAIN_CLASS="com.nicksxs.bytecode.demo.app.DemoApplication"
VERSION="1.0.0"

case "$MODE" in
  baseline)
    exec "$JAVA_BIN" -cp "$CLASSPATH" "$MAIN_CLASS" "$ITERATIONS" "$PAUSE_MILLIS"
    ;;
  asm)
    exec "$JAVA_BIN" \
      -javaagent:"$ROOT_DIR/target/asm-bytekit-arthas-demo-$VERSION-asm-agent.jar" \
      -cp "$CLASSPATH" "$MAIN_CLASS" "$ITERATIONS" "$PAUSE_MILLIS"
    ;;
  bytekit)
    exec "$JAVA_BIN" \
      -javaagent:"$ROOT_DIR/target/asm-bytekit-arthas-demo-$VERSION-bytekit-agent.jar" \
      -cp "$CLASSPATH" "$MAIN_CLASS" "$ITERATIONS" "$PAUSE_MILLIS"
    ;;
  app)
    exec "$JAVA_BIN" -cp "$CLASSPATH" "$MAIN_CLASS" 0 1000
    ;;
  *)
    echo "usage: $0 {baseline|asm|bytekit|app} [iterations] [pauseMillis]" >&2
    exit 1
    ;;
esac
