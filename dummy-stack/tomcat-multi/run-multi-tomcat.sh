#!/usr/bin/env bash
set -euo pipefail

IFS=',' read -r -a INSTANCES <<< "${TOMCAT_INSTANCES:-payments,services,auth}"
if [[ ${#INSTANCES[@]} -eq 0 ]]; then
  echo "TOMCAT_INSTANCES cannot be empty." >&2
  exit 1
fi

BASE_HTTP_PORT="${BASE_HTTP_PORT:-8081}"
BASE_SHUTDOWN_PORT="${BASE_SHUTDOWN_PORT:-8101}"
SERVER_LABEL="${TOMCAT_SERVER_LABEL:-dummy-stack}"

to_env_key() {
  local instance="$1"
  instance="${instance//-/_}"
  instance="${instance^^}"
  echo "${instance}"
}

create_webapps() {
  local catalina_base="$1"
  local instance="$2"

  local key
  key="$(to_env_key "${instance}")"
  local var_name="WEBAPPS_${key}"
  local raw="${!var_name:-}"
  raw="${raw// /}"
  [[ -z "${raw}" ]] && return 0

  IFS=',' read -r -a apps <<< "${raw}"
  for app in "${apps[@]}"; do
    [[ -z "${app}" ]] && continue
    local app_dir="${catalina_base}/webapps/${app}"
    mkdir -p "${app_dir}"
    cat > "${app_dir}/index.jsp" <<EOF
<%@ page contentType="text/html; charset=UTF-8" %>
<!doctype html>
<html lang="en">
  <head><meta charset="utf-8"><title>${app}</title></head>
  <body>
    <h1>${app}</h1>
    <p>server: ${SERVER_LABEL}</p>
    <p>instance: ${instance}</p>
  </body>
</html>
EOF
  done
}

prepare_instance() {
  local instance="$1"
  local index="$2"
  local http_port=$((BASE_HTTP_PORT + index))
  local shutdown_port=$((BASE_SHUTDOWN_PORT + index))
  local catalina_base="/opt/tomcats/instances/${instance}"

  mkdir -p "${catalina_base}/conf" "${catalina_base}/webapps" "${catalina_base}/logs" "${catalina_base}/temp" "${catalina_base}/work"

  cp -R /opt/tomcat-template/conf/. "${catalina_base}/conf/"
  rm -rf "${catalina_base}/webapps/ROOT" "${catalina_base}/webapps/manager"
  cp -R /opt/tomcat-template/webapps/ROOT "${catalina_base}/webapps/ROOT"
  cp -R /opt/tomcat-template/webapps/manager "${catalina_base}/webapps/manager"

  sed -i -E "s/port=\"[0-9]+\" shutdown=\"SHUTDOWN\"/port=\"${shutdown_port}\" shutdown=\"SHUTDOWN\"/" "${catalina_base}/conf/server.xml"
  sed -i -E "0,/Connector port=\"[0-9]+\" protocol=\"HTTP\\/1\\.1\"/{s/Connector port=\"[0-9]+\" protocol=\"HTTP\\/1\\.1\"/Connector port=\"${http_port}\" protocol=\"HTTP\\/1.1\"/}" "${catalina_base}/conf/server.xml"

  create_webapps "${catalina_base}" "${instance}"
}

pids=()
for i in "${!INSTANCES[@]}"; do
  prepare_instance "${INSTANCES[$i]}" "${i}"
done

for instance in "${INSTANCES[@]}"; do
  catalina_base="/opt/tomcats/instances/${instance}"
  CATALINA_HOME=/usr/local/tomcat \
  CATALINA_BASE="${catalina_base}" \
  CATALINA_TMPDIR="${catalina_base}/temp" \
    /usr/local/tomcat/bin/catalina.sh run &
  pids+=("$!")
done

stop_all() {
  for pid in "${pids[@]:-}"; do
    if kill -0 "${pid}" >/dev/null 2>&1; then
      kill "${pid}" >/dev/null 2>&1 || true
    fi
  done
}

trap stop_all EXIT INT TERM

wait -n "${pids[@]}"
status=$?
stop_all
exit "${status}"
