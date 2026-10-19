import { useEffect, useState, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import { orderApi, paymentApi } from '../api/client'
import { formatPrice, formatDate, shortId } from '../utils/helpers'

const POLL_INTERVAL_MS = 3000
const TERMINAL_STATUSES = ['PAID', 'CANCELLED', 'PAYMENT_FAILED']

const STATUS_LABEL = {
  PENDING: '⏳ Chờ thanh toán',
  PAID: '✅ Thanh toán thành công',
  CANCELLED: '❌ Đã huỷ',
  PAYMENT_FAILED: '❌ Thanh toán thất bại',
}

const STATUS_DESC = {
  PENDING: 'Đơn hàng đang được xử lý qua Kafka Saga. Tự động cập nhật...',
  PAID: 'Thanh toán đã được xác nhận. Đơn hàng của bạn sẽ sớm được giao!',
  CANCELLED: 'Đơn hàng đã bị huỷ.',
  PAYMENT_FAILED: 'Thanh toán thất bại. Vui lòng thử lại.',
}

export default function OrderDetailPage() {
  const { id } = useParams()
  const [order, setOrder] = useState(null)
  const [payment, setPayment] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [pollCount, setPollCount] = useState(0)
  const intervalRef = useRef(null)

  const fetchOrder = async () => {
    try {
      const { data } = await orderApi.get(id)
      setOrder(data)
      if (TERMINAL_STATUSES.includes(data.status)) {
        clearInterval(intervalRef.current)
      }
      try {
        const { data: payData } = await paymentApi.getByOrder(id)
        if (payData) setPayment(payData)
      } catch {
        // Payment might still be pending
      }
    } catch (err) {
      // Fallback: Kiểm tra mock order trong localStorage
      const mockOrders = JSON.parse(localStorage.getItem('mock_orders_data') || '{}')
      if (mockOrders[id]) {
        const current = mockOrders[id]
        // Mô phỏng tiến trình Saga: sau 2 lần poll (khoảng 6s)
        // Nếu số tiền tận cùng .13 -> CANCELLED (INSUFFICIENT_FUNDS)
        // Ngược lại -> PAID
        if (current.status === 'PENDING' && pollCount >= 2) {
          const totalStr = Number(current.totalAmount).toFixed(2)
          if (totalStr.endsWith('.13')) {
            current.status = 'CANCELLED'
            current.cancelReason = 'Mock Gateway: INSUFFICIENT_FUNDS (Thanh toán bị từ chối)'
          } else {
            current.status = 'PAID'
          }
          mockOrders[id] = current
          localStorage.setItem('mock_orders_data', JSON.stringify(mockOrders))
          clearInterval(intervalRef.current)
        }
        setOrder({ ...current })
        if (current.status !== 'PENDING') {
          setPayment({
            id: 'pay-' + (id.length > 8 ? id.slice(0, 8) : id),
            orderId: id,
            amount: current.totalAmount,
            currency: current.currency,
            status: current.status === 'PAID' ? 'COMPLETED' : 'FAILED',
            gatewayReference: 'gw_' + (id.length > 8 ? id.slice(0, 8) : id),
            failureReason: current.cancelReason || null,
            createdAt: current.createdAt,
          })
        }
      } else {
        setError('Không tìm thấy đơn hàng hoặc bạn không có quyền xem.')
        clearInterval(intervalRef.current)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchOrder()

    // Poll until terminal status
    intervalRef.current = setInterval(() => {
      setPollCount((c) => c + 1)
      fetchOrder()
    }, POLL_INTERVAL_MS)

    return () => clearInterval(intervalRef.current)
  }, [id])

  if (loading) {
    return (
      <div className="order-detail">
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '80px 0' }}>
          <span className="spinner spinner--lg" style={{ color: 'var(--color-primary)' }} />
          <p className="text-muted">Đang tải đơn hàng...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="order-detail">
        <div className="alert alert--error">{error}</div>
        <Link to="/orders" className="btn btn--ghost mt-4" style={{ display: 'inline-flex' }}>
          ← Về danh sách đơn hàng
        </Link>
      </div>
    )
  }

  const isTerminal = TERMINAL_STATUSES.includes(order.status)
  const isPending = order.status === 'PENDING'

  return (
    <div className="order-detail">
      {/* Polling banner */}
      {isPending && (
        <div className="polling-banner">
          <span className="spinner" />
          <span>
            Đang theo dõi trạng thái realtime — polling mỗi {POLL_INTERVAL_MS / 1000}s
            {pollCount > 0 && ` (lần ${pollCount})`}
          </span>
        </div>
      )}

      <div className="order-detail__card">
        {/* Header */}
        <div className="order-detail__header">
          <div>
            <h1 className="page-title" style={{ fontSize: '1.4rem', marginBottom: 6 }}>
              Chi tiết đơn hàng
            </h1>
            <p className="order-id">#{order.id}</p>
            <p style={{ fontSize: '0.8rem', color: 'var(--color-text-3)', marginTop: 4 }}>
              {formatDate(order.createdAt)}
            </p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <span className={`status-badge status-badge--${order.status}`}>
              <span className="status-dot" />
              {order.status}
            </span>
            <p style={{
              fontSize: '0.75rem',
              color: 'var(--color-text-3)',
              marginTop: 8,
              maxWidth: 200,
              textAlign: 'right',
            }}>
              {STATUS_DESC[order.status]}
            </p>
          </div>
        </div>

        {/* Status explanation */}
        <div className={`alert alert--${isPending ? 'warning' : order.status === 'PAID' ? 'success' : 'error'}`}
             style={{ marginBottom: 24 }}>
          {STATUS_LABEL[order.status]}
        </div>

        {/* Cancel reason */}
        {order.cancelReason && (
          <div className="alert alert--error" style={{ marginBottom: 16, fontSize: '0.85rem' }}>
            <strong>Lý do huỷ:</strong> {order.cancelReason}
          </div>
        )}

        {/* Items */}
        <h2 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--color-text-2)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 12 }}>
          Sản phẩm đã đặt
        </h2>
        <div className="order-items-list">
          {order.items.map((item, idx) => (
            <div key={idx} className="order-item-row">
              <div>
                <div className="order-item-row__name">{item.productName || item.productId}</div>
                <div className="order-item-row__meta">
                  SL: {item.quantity} × {formatPrice(item.unitPrice, order.currency)}
                </div>
              </div>
              <div className="order-item-row__price">
                {formatPrice(item.unitPrice * item.quantity, order.currency)}
              </div>
            </div>
          ))}
        </div>

        {/* Total */}
        <div className="order-totals">
          <div className="order-total-row">
            <span>Tổng cộng</span>
            <span>{formatPrice(order.totalAmount, order.currency)}</span>
          </div>
        </div>

        {/* 💳 Payment Details Card */}
        <div style={{
          marginTop: 24,
          padding: 18,
          background: 'rgba(99, 102, 241, 0.05)',
          border: '1px solid rgba(99, 102, 241, 0.25)',
          borderRadius: 'var(--radius-md)',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
            <h2 style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--color-primary-light)', textTransform: 'uppercase', letterSpacing: '0.05em', margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>💳</span> Thông tin thanh toán (Payment Gateway)
            </h2>
            {payment ? (
              <span className={`status-badge status-badge--${payment.status === 'COMPLETED' ? 'PAID' : payment.status === 'FAILED' ? 'CANCELLED' : 'PENDING'}`}>
                <span className="status-dot" />
                {payment.status === 'COMPLETED' ? 'PAID (Thành công)' : payment.status === 'FAILED' ? 'FAILED (Thất bại)' : 'PENDING'}
              </span>
            ) : isPending ? (
              <span className="status-badge status-badge--PENDING">
                <span className="spinner spinner--sm" /> Đang gửi lệnh...
              </span>
            ) : (
              <span className="status-badge status-badge--PAID">
                <span className="status-dot" /> ĐÃ XÁC NHẬN
              </span>
            )}
          </div>

          {payment ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14, fontSize: '0.85rem' }}>
              <div>
                <span style={{ color: 'var(--color-text-3)', display: 'block', fontSize: '0.75rem', marginBottom: 2 }}>Mã giao dịch (Payment ID)</span>
                <span style={{ fontFamily: 'monospace', color: 'var(--color-text)', wordBreak: 'break-all' }}>{payment.id}</span>
              </div>
              <div>
                <span style={{ color: 'var(--color-text-3)', display: 'block', fontSize: '0.75rem', marginBottom: 2 }}>Cổng thanh toán</span>
                <span style={{ color: 'var(--color-text)', fontWeight: 600 }}>Mock Gateway (Kafka Saga)</span>
              </div>
              <div>
                <span style={{ color: 'var(--color-text-3)', display: 'block', fontSize: '0.75rem', marginBottom: 2 }}>Mã đối soát Gateway Ref</span>
                <span style={{ fontFamily: 'monospace', color: 'var(--color-primary-light)' }}>{payment.gatewayReference || 'gw_auto_settled'}</span>
              </div>
              <div>
                <span style={{ color: 'var(--color-text-3)', display: 'block', fontSize: '0.75rem', marginBottom: 2 }}>Số tiền thanh toán</span>
                <span style={{ fontWeight: 700, color: 'var(--color-text)' }}>{formatPrice(payment.amount, payment.currency)}</span>
              </div>
              {payment.createdAt && (
                <div>
                  <span style={{ color: 'var(--color-text-3)', display: 'block', fontSize: '0.75rem', marginBottom: 2 }}>Thời gian giao dịch</span>
                  <span style={{ color: 'var(--color-text-2)' }}>{formatDate(payment.createdAt)}</span>
                </div>
              )}
              {payment.failureReason && (
                <div style={{ gridColumn: '1 / -1', background: 'rgba(239, 68, 68, 0.1)', padding: '8px 12px', borderRadius: 'var(--radius-sm)' }}>
                  <span style={{ color: 'var(--color-danger)', display: 'block', fontSize: '0.75rem', fontWeight: 600 }}>Lý do từ chối giao dịch:</span>
                  <span style={{ color: 'var(--color-danger)', fontWeight: 700 }}>{payment.failureReason}</span>
                </div>
              )}
            </div>
          ) : (
            <div style={{ fontSize: '0.82rem', color: 'var(--color-text-3)', display: 'flex', alignItems: 'center', gap: 8 }}>
              {isPending ? (
                <>
                  <span className="spinner" />
                  <span>Đang kết nối Kafka Saga & cổng thanh toán... Trạng thái sẽ cập nhật tự động.</span>
                </>
              ) : (
                <span>Giao dịch thanh toán đã được ghi nhận tự động cho đơn hàng này.</span>
              )}
            </div>
          )}
        </div>

        {/* 📬 Dev Mail Action Banner when PENDING */}
        {isPending && (
          <div style={{
            marginTop: 20,
            padding: 16,
            background: 'rgba(245, 158, 11, 0.08)',
            border: '1px solid rgba(245, 158, 11, 0.35)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 14,
          }}>
            <div style={{ maxWidth: 440 }}>
              <div style={{ fontWeight: 700, color: '#f59e0b', fontSize: '0.9rem', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
                <span>📬</span> Thanh toán qua Email (Dev Mail)
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--color-text-2)', lineHeight: 1.5 }}>
                Email xác nhận đã được gửi tới <strong>Mailpit</strong>. Bạn chỉ cần mở hộp thư và bấm nút <strong>Accept</strong> để đơn hàng chuyển sang <strong>PAID</strong>!
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
              <a
                href="http://localhost:8025"
                target="_blank"
                rel="noreferrer"
                className="btn btn--sm"
                style={{
                  background: '#f59e0b',
                  color: '#0f172a',
                  fontWeight: 700,
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '8px 14px',
                  borderRadius: 'var(--radius-sm)',
                  textDecoration: 'none'
                }}
              >
                <span>📧</span> Mở Mailpit (:8025) ↗
              </a>
              <button
                className="btn btn--ghost btn--sm"
                onClick={async () => {
                  try {
                    await paymentApi.approve(order.id)
                    fetchOrder()
                  } catch (e) {
                    alert('Lỗi: ' + e.message)
                  }
                }}
              >
                ⚡ Duyệt ngay
              </button>
            </div>
          </div>
        )}

        {/* Actions */}
        <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
          <Link to="/orders">
            <button className="btn btn--ghost" id="back-to-orders-btn">
              ← Tất cả đơn hàng
            </button>
          </Link>
          <Link to="/">
            <button className="btn btn--primary" id="continue-shopping-btn">
              🛍️ Tiếp tục mua sắm
            </button>
          </Link>
        </div>
      </div>

      {/* Kafka Flow Diagram */}
      <div style={{
        marginTop: 24,
        padding: 20,
        background: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-md)',
        fontSize: '0.8rem',
        color: 'var(--color-text-3)',
      }}>
        <p style={{ fontWeight: 700, color: 'var(--color-text-2)', marginBottom: 8 }}>
          🔄 Kafka Saga Flow
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          {[
            { label: 'Order Created', done: true },
            { label: '→' },
            { label: 'payment.request (Kafka)', done: true },
            { label: '→' },
            { label: 'Payment Processing', done: order.status !== 'PENDING' },
            { label: '→' },
            { label: 'payment.result (Kafka)', done: order.status !== 'PENDING' },
            { label: '→' },
            { label: order.status === 'PAID' ? 'PAID ✅' : order.status === 'PAYMENT_FAILED' ? 'FAILED ❌' : 'Waiting...', done: isTerminal },
          ].map((step, i) =>
            step.label === '→' ? (
              <span key={i} style={{ color: 'var(--color-border-2)' }}>→</span>
            ) : (
              <span
                key={i}
                style={{
                  padding: '3px 8px',
                  borderRadius: 4,
                  background: step.done ? 'rgba(99,102,241,0.15)' : 'var(--color-surface-2)',
                  color: step.done ? 'var(--color-primary)' : 'var(--color-text-3)',
                  border: `1px solid ${step.done ? 'rgba(99,102,241,0.3)' : 'var(--color-border)'}`,
                  fontWeight: step.done ? 600 : 400,
                }}
              >
                {step.label}
              </span>
            )
          )}
        </div>
      </div>
    </div>
  )
}
