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
  local container_name="zapscan-${label}-$$"

  echo "==> Сканирование [$label]: $target"

  local docker_args=(zap-baseline.py -t "$target" -r "${report_name}.html" -J "${report_name}.json" -I)
  if [ -n "$ZAP_EXTRA_OPTS" ]; then
    docker_args+=(-z "$ZAP_EXTRA_OPTS")
  fi

  # Намеренно БЕЗ bind-mount: ZAP пишет отчёты в свой внутренний /zap/wrk, где у
  # пользователя контейнера (uid 1000 "zap") есть права на запись. Монтирование
  # каталога хоста сюда ломает запись, так как uid контейнера != uid runner'а
  # (на это не помогает ни chmod, ни --user — путь /home/runner/... недоступен
  # стороннему uid). Готовые файлы достаём через docker cp (работает от имени
  # docker-демона, минуя любые ограничения прав).
  docker rm -f "$container_name" >/dev/null 2>&1 || true
  docker run --name "$container_name" -t --network host \
    "$ZAP_IMAGE" "${docker_args[@]}"
  local status=$?

  docker cp "$container_name:/zap/wrk/${report_name}.html" "$REPORT_DIR/" 2>/dev/null \
    || echo "  внимание: HTML-отчёт [$label] не создан" >&2
  docker cp "$container_name:/zap/wrk/${report_name}.json" "$REPORT_DIR/" 2>/dev/null \
    || echo "  внимание: JSON-отчёт [$label] не создан" >&2
  docker rm -f "$container_name" >/dev/null 2>&1 || true

  if [ $status -ne 0 ]; then
    echo "==> ZAP [$label] завершился с кодом $status (найдены предупреждения/уязвимости) — см. $REPORT_DIR/${report_name}.html"
  fi
  return 0
}

run_scan "$API_URL" "api"
run_scan "$FRONTEND_URL" "frontend"

echo "==> Готово. Отчёты сохранены в $REPORT_DIR"
ls -la "$REPORT_DIR"

# Если ни одного отчёта не появилось — это ошибка сканирования, а не «успех без
# артефактов»: падаем явно, чтобы прогон CI стал красным и проблема была видна.
if ! ls "$REPORT_DIR"/*.html >/dev/null 2>&1; then
  echo "ОШИБКА: не создано ни одного HTML-отчёта ZAP" >&2
  exit 1
fi
