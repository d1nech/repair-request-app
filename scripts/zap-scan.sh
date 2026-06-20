#!/usr/bin/env bash
# Воспроизводимый запуск фаззинг-тестирования OWASP ZAP (baseline scan)
# против backend REST API и статического frontend.
#
# Требования: запущенное приложение (docker compose up -d) и Docker для самого ZAP.
#
# Переменные окружения (необязательные):
#   API_URL        - адрес backend (по умолчанию http://localhost:8080)
#   FRONTEND_URL   - адрес frontend (по умолчанию http://localhost:3000)
#   ADMIN_EMAIL    - email тестового администратора для авторизованного скана
#   ADMIN_PASSWORD - пароль тестового администратора
#   ZAP_IMAGE      - образ ZAP (по умолчанию ghcr.io/zaproxy/zaproxy:stable)
#
# Результат: HTML и JSON отчёты в docs/zap/reports/zap-<host>-<timestamp>.{html,json}

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
REPORT_DIR="$REPO_ROOT/docs/zap/reports"

API_URL="${API_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:3000}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin12345}"
ZAP_IMAGE="${ZAP_IMAGE:-ghcr.io/zaproxy/zaproxy:stable}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$REPORT_DIR"
# Образ ghcr.io/zaproxy/zaproxy запускается под непривилегированным пользователем
# внутри контейнера (uid 1000), который иначе не может писать в смонтированную
# директорию хоста (например, на runner'е GitHub Actions она создаётся с правами 755).
chmod 777 "$REPORT_DIR"

echo "==> Получение JWT-токена администратора для авторизованного скана"
LOGIN_RESPONSE="$(curl -sf -X POST "$API_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" || true)"
TOKEN="$(printf '%s' "$LOGIN_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"

ZAP_EXTRA_OPTS=""
if [ -n "$TOKEN" ]; then
  echo "==> Токен получен, запросы будут авторизованы заголовком Authorization"
  ZAP_EXTRA_OPTS="-config replacer.full_list(0).description=auth -config replacer.full_list(0).enabled=true -config replacer.full_list(0).matchtype=REQ_HEADER -config replacer.full_list(0).matchstr=Authorization -config replacer.full_list(0).regex=false -config replacer.full_list(0).replacement=Bearer\\ $TOKEN"
else
  echo "==> Не удалось получить токен, сканирование пройдёт без авторизации (см. ADMIN_EMAIL/ADMIN_PASSWORD)" >&2
fi

run_scan() {
  local target="$1"
  local label="$2"
  local report_name="zap-${label}-${TIMESTAMP}"

  echo "==> Сканирование [$label]: $target"

  local docker_args=(zap-baseline.py -t "$target" -r "${report_name}.html" -J "${report_name}.json" -I)
  if [ -n "$ZAP_EXTRA_OPTS" ]; then
    docker_args+=(-z "$ZAP_EXTRA_OPTS")
  fi

  # --user сопоставляет процесс в контейнере с хостовым пользователем-владельцем
  # $REPORT_DIR (по умолчанию образ работает под uid 1000 "zap", у которого нет
  # доступа к смонтированной директории на раннере GitHub Actions); HOME
  # переопределён на смонтированный каталог, так как zap.sh пишет туда свои файлы.
  docker run --rm -t --network host \
    -v "$REPORT_DIR":/zap/wrk/:rw \
    --user "$(id -u):$(id -g)" \
    -e HOME=/zap/wrk \
    "$ZAP_IMAGE" "${docker_args[@]}"
  local status=$?

  if [ $status -ne 0 ]; then
    echo "==> ZAP завершился с кодом $status (найдены предупреждения/уязвимости) — см. $REPORT_DIR/${report_name}.html"
  fi
  return 0
}

run_scan "$API_URL" "api"
run_scan "$FRONTEND_URL" "frontend"

echo "==> Готово. Отчёты сохранены в $REPORT_DIR"
