import { useEffect, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { orderApi } from '../api/client'
import { formatPrice, formatDate, shortId } from '../utils/helpers'

export default function OrdersPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Note: The backend doesn't expose a "list orders" endpoint yet.
  // We track order IDs in localStorage as a workaround.
  useEffect(() => {
    if (!user) { navigate('/login'); return }

    const savedIds = JSON.parse(localStorage.getItem('order_ids') || '[]')
    if (savedIds.length === 0) { setLoading(false); return }

    setLoading(true)
    const mockOrders = JSON.parse(localStorage.getItem('mock_orders_data') || '{}')
    Promise.allSettled(savedIds.map((id) => orderApi.get(id)))
      .then((results) => {
        const fetched = results.map((r, idx) => {
          if (r.status === 'fulfilled') return r.value.data
          const id = savedIds[idx]
          return mockOrders[id] || null
        }).filter(Boolean)
          .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        setOrders(fetched)
      })
      .catch(() => {
        // Fallback to local mock orders
        const localList = savedIds.map((id) => mockOrders[id]).filter(Boolean)
        setOrders(localList)
      })
      .finally(() => setLoading(false))
  }, [user])

  const statusColor = {
    PENDING: 'var(--color-warning)',
    PAID: 'var(--color-success)',
    CANCELLED: 'var(--color-danger)',
    PAYMENT_FAILED: 'var(--color-danger)',
  }

  return (
    <div className="container">
      <div className="page-header">
        <h1 className="page-title">📋 Đơn hàng của tôi</h1>
        <p className="page-subtitle">Theo dõi trạng thái tất cả đơn hàng</p>
      </div>

      {error && <div className="alert alert--error mb-4">{error}</div>}

      {loading ? (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, color: 'var(--color-text-2)' }}>
          <span className="spinner" style={{ color: 'var(--color-primary)' }} />
          Đang tải đơn hàng...
        </div>
      ) : orders.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '80px 0' }}>
          <div style={{ fontSize: '3rem', marginBottom: 16 }}>📭</div>
          <p style={{ color: 'var(--color-text-2)', marginBottom: 16 }}>
            Bạn chưa có đơn hàng nào
          </p>
          <Link to="/">
            <button className="btn btn--primary" id="start-shopping-btn">
              🛍️ Bắt đầu mua sắm
            </button>
          </Link>
        </div>
      ) : (
        <div className="orders-list">
          {orders.map((order) => (
            <div
              key={order.id}
              className="order-row"
              id={`order-row-${order.id}`}
              onClick={() => navigate(`/orders/${order.id}`)}
            >
              <div className="order-row__left">
                <span style={{ fontWeight: 700, fontSize: '0.95rem' }}>
                  Đơn #{shortId(order.id)}
                </span>
                <span className="order-row__id">{order.id}</span>
                <span className="order-row__date">{formatDate(order.createdAt)}</span>
                <span style={{ fontSize: '0.8rem', color: 'var(--color-text-3)' }}>
                  {order.items.length} sản phẩm
                </span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8 }}>
                <span className={`status-badge status-badge--${order.status}`}>
                  <span className="status-dot" />
                  {order.status}
                </span>
                <span className="order-row__amount">
                  {formatPrice(order.totalAmount, order.currency)}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
