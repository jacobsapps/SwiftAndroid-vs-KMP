#!/usr/bin/env bash

# Ensures a compatible JDK is available before invoking the Gradle embed task.
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

log() {
  printf '[run-kmp-gradle] %s\n' "$*" >&2
}

resolve_java_home() {
  /usr/libexec/java_home "$@" 2>/dev/null || true
}

extract_java_version() {
  local candidate="$1"
  local output version
  output="$("${candidate}/bin/java" -version 2>&1 || true)"
  version="$(printf '%s\n' "${output}" | awk -F '\"' '/version/ {print $2; exit}')"
  if [[ -z "${version}" ]]; then
    version="$(printf '%s\n' "${output}" | awk '{for (i = 1; i <= NF; ++i) if ($i ~ /^[0-9]+(\.[0-9]+)*$/) { print $i; exit }}')"
  fi
  printf '%s' "${version}"
}

UNSUPPORTED_CANDIDATES=()

is_compatible_java_home() {
  local candidate="$1"
  [[ -n "${candidate}" && -x "${candidate}/bin/java" ]] || return 1
  local version
  version="$(extract_java_version "${candidate}")"
  [[ -n "${version}" ]] || return 1
  local major="${version%%.*}"
  if [[ "${major}" == "1" ]]; then
    major="$(echo "${version}" | awk -F '.' '{print $2}')"
  fi
  if [[ "${major}" -lt 17 ]]; then
    return 1
  fi
  if [[ "${major}" -gt 21 ]]; then
    log "Rejecting ${candidate} because Java ${major} is newer than supported (need 17-21)."
    UNSUPPORTED_CANDIDATES+=("${candidate} (java ${version})")
    return 1
  fi
  return 0
}

find_java_home() {
  log "Existing JAVA_HOME env: ${JAVA_HOME:-<unset>}"
  local candidates=()
  if [[ -n "${JAVA_HOME:-}" ]]; then
    candidates+=("${JAVA_HOME}")
  fi
  candidates+=("$(resolve_java_home -v 21)")
  candidates+=("$(resolve_java_home -v 17)")
  candidates+=("$(resolve_java_home)")
  if command -v java >/dev/null 2>&1; then
    local java_bin
    java_bin="$(command -v java)"
    candidates+=("$(cd "$(dirname "${java_bin}")/.." && pwd)")
  fi

  for candidate in "${candidates[@]}"; do
    [[ -n "${candidate}" ]] || continue
    log "Testing JAVA_HOME candidate: ${candidate}"
    if is_compatible_java_home "${candidate}"; then
      local version
      version="$(extract_java_version "${candidate}")"
      log "Selected JAVA_HOME=${candidate} (java version ${version})"
      echo "${candidate}"
      return 0
    fi
    log "Rejected candidate: ${candidate} (version $(extract_java_version "${candidate}" || printf '<unknown>'))"
  done
  log "No compatible JAVA_HOME candidates found"
  return 1
}

JAVA_HOME="$(find_java_home || true)"

if [[ -z "${JAVA_HOME}" ]]; then
  if [[ "${#UNSUPPORTED_CANDIDATES[@]}" -gt 0 ]]; then
    log "Found newer JDKs that are unsupported: ${UNSUPPORTED_CANDIDATES[*]}"
  fi
  cat <<'EOF' >&2
[run-kmp-gradle] error: Unable to locate a compatible JDK in the supported range (17-21 required for the Kotlin Gradle build).
[run-kmp-gradle] Install JDK 21 LTS or JDK 17 LTS (for example via https://adoptium.net/temurin/releases/) or ensure JAVA_HOME points to one of them, then retry.
EOF
  exit 1
fi

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

log "Final JAVA_HOME=${JAVA_HOME}"
log "java -version output:"
"${JAVA_HOME}/bin/java" -version >&2 || log "Failed to execute java -version"

cd "${PROJECT_ROOT}"
log "Invoking Gradle from ${PROJECT_ROOT}"

./gradlew :shared:embedAndSignAppleFrameworkForXcode "$@"
