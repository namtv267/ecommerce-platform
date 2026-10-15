#!/usr/bin/env bash
# Demo end-to-end. Yêu cầu: docker compose đã chạy xong, có curl và jq.
set -euo pipefail

AUTH=${AUTH:-http://localhost:8081}
ORDER=${ORDER:-http://localhost:8083}
PAYMENT=${PAYMENT:-http://localhost:8084}
EMAIL="demo-$RANDOM@example.com"
RUN=$RANDOM

say() { printf '\n\033[1;36m== %s\033[0m\n' "$*"; }

say "1. Đăng ký + đăng nhập ($EMAIL)"
curl -s -X POST "$AUTH/api/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"fullName\":\"Demo\"}" | jq -c .
TOKEN=$(curl -s -X POST "$AUTH/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"password123\"}" | jq -r .accessToken)

place() { # $1=idempotency-key  $2=json body  -> in ra "HTTP_STATUS BODY"
  curl -s -o /tmp/demo_body -D /tmp/demo_headers -w '%{http_code}' -X POST "$ORDER/api/orders" \
    -H "Authorization: Bearer $TOKEN" -H "Idempotency-Key: $1" -H 'Content-Type: application/json' -d "$2"
}
wait_status() { # $1=orderId : đợi tới khi khác PENDING
  for _ in $(seq 1 30); do
    S=$(curl -s "$ORDER/api/orders/$1" -H "Authorization: Bearer $TOKEN" | jq -r .status)
    [ "$S" != "PENDING" ] && { echo "$S"; return; }
    sleep 1
  done
  echo "PENDING (timeout)"
}

BODY='{"items":[{"productId":"prod-keyboard","quantity":1},{"productId":"prod-mouse","quantity":2}]}'

say "2. Đặt hàng lần đầu (Idempotency-Key = demo-$RUN-a)"
CODE=$(place "demo-$RUN-a" "$BODY"); ID1=$(jq -r .id /tmp/demo_body)
echo "HTTP $CODE  order=$ID1  status=$(jq -r .status /tmp/demo_body)  total=$(jq -r .totalAmount /tmp/demo_body)"

say "3. Gửi LẠI cùng key + cùng nội dung -> phải ra cùng đơn, có header Idempotent-Replayed"
CODE=$(place "demo-$RUN-a" "$BODY"); ID2=$(jq -r .id /tmp/demo_body)
echo "HTTP $CODE  order=$ID2  replayed=$(grep -i '^idempotent-replayed' /tmp/demo_headers | tr -d '\r' || echo no)"
[ "$ID1" = "$ID2" ] && echo "OK: cùng 1 đơn" || { echo "FAIL: khác đơn!"; exit 1; }

say "4. Cùng key nhưng KHÁC nội dung -> 422"
CODE=$(place "demo-$RUN-a" '{"items":[{"productId":"prod-monitor","quantity":1}]}')
echo "HTTP $CODE  $(jq -c '{title,detail}' /tmp/demo_body)"

say "5. 8 request SONG SONG cùng 1 key mới -> chỉ được tạo đúng 1 đơn"
for i in $(seq 1 8); do
  ( curl -s -o "/tmp/demo_par_$i" -w '%{http_code}\n' -X POST "$ORDER/api/orders" \
      -H "Authorization: Bearer $TOKEN" -H "Idempotency-Key: demo-$RUN-par" \
      -H 'Content-Type: application/json' -d "$BODY" > "/tmp/demo_par_code_$i" ) &
done
wait
echo "Mã HTTP:  $(cat /tmp/demo_par_code_* | sort | uniq -c | tr '\n' ' ')   (202 = thành công / replay, 409 = đang xử lý, retry sau)"
echo "Số đơn khác nhau tạo ra: $(cat /tmp/demo_par_[0-9]* 2>/dev/null | jq -r '.id? // empty' | sort -u | wc -l)"

say "6. Saga: đợi đơn đầu tiên được xác nhận"
echo "Order $ID1 -> $(wait_status "$ID1")"
echo "Payment: $(curl -s "$PAYMENT/api/payments/order/$ID1" -H "Authorization: Bearer $TOKEN" | jq -c '{status,gatewayReference,amount}')"

say "7. Bù trừ: sản phẩm giá .13 -> payment bị từ chối -> đơn CANCELLED"
place "demo-$RUN-decline" '{"items":[{"productId":"prod-decline-test","quantity":1}]}' >/dev/null
ID3=$(jq -r .id /tmp/demo_body)
echo "Order $ID3 -> $(wait_status "$ID3")  reason=$(curl -s "$ORDER/api/orders/$ID3" -H "Authorization: Bearer $TOKEN" | jq -r .cancelReason)"

say "8. Gateway timeout SAU khi trừ tiền (giá .14): retry cùng key -> vẫn CONFIRMED, chỉ trừ 1 lần"
place "demo-$RUN-flaky" '{"items":[{"productId":"prod-flaky-test","quantity":1}]}' >/dev/null
ID4=$(jq -r .id /tmp/demo_body)
echo "Order $ID4 -> $(wait_status "$ID4")   (xem log payment-service: docker compose logs payment-service)"

say "9. Hết hàng -> 409"
CODE=$(place "demo-$RUN-oos" '{"items":[{"productId":"prod-hub","quantity":1}]}')
echo "HTTP $CODE  $(jq -r .detail /tmp/demo_body)"
