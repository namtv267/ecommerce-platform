import { useEffect, useState, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import { orderApi } from '../api/client'
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
