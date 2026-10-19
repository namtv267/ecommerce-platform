import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'
import { useNavigate } from 'react-router-dom'
import { orderApi } from '../api/client'
import { useState } from 'react'

export default function CartPanel() {
  const { items, total, isOpen, setIsOpen, updateQty, removeItem, clearCart } = useCart()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [placing, setPlacing] = useState(false)
  const [error, setError] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('CARD')

  const handleCheckout = async () => {
    if (!user) { navigate('/login'); setIsOpen(false); return }
    if (items.length === 0) return

    setPlacing(true)
    setError('')
    try {
      const payload = items.map((i) => ({
        productId: i.productId,
        quantity: i.quantity,
      }))
      let orderId
      const isEmail = paymentMethod === 'EMAIL'
      try {
        const { data } = await orderApi.place(payload, isEmail)
        orderId = data.id
      } catch (backendErr) {
        // Fallback: Tạo mock order mô phỏng trong localStorage nếu backend chưa chạy
        const mockId = 'ord-' + crypto.randomUUID().slice(0, 8)
        const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0)
        const mockOrder = {
          id: mockId,
          customerId: user.id || 'demo-user',
          items: items.map((i) => ({
            productId: i.productId,
            productName: i.name,
            name: i.name,
            quantity: i.quantity,
            unitPrice: i.price,
            price: i.price,
          })),
          totalAmount: total,
          currency: items[0]?.currency || 'USD',
          status: 'PENDING',
          paymentMethod: paymentMethod,
          createdAt: new Date().toISOString(),
          isMock: true,
        }
        const mockOrders = JSON.parse(localStorage.getItem('mock_orders_data') || '{}')
        mockOrders[mockId] = mockOrder
        localStorage.setItem('mock_orders_data', JSON.stringify(mockOrders))
        orderId = mockId
      }

      const prev = JSON.parse(localStorage.getItem('order_ids') || '[]')
      localStorage.setItem('order_ids', JSON.stringify([orderId, ...prev].slice(0, 50)))
      clearCart()
      setIsOpen(false)
      navigate(`/orders/${orderId}`)
    } catch (err) {
      setError(err.response?.data?.message || 'Đặt hàng thất bại, thử lại!')
    } finally {
      setPlacing(false)
    }
  }

  const currency = items[0]?.currency || 'USD'

  return (
    <>
      {/* Overlay */}
      {isOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            zIndex: 89,
          }}
          onClick={() => setIsOpen(false)}
        />
      )}

      <aside className={`cart-panel ${isOpen ? 'cart-panel--open' : ''}`} id="cart-panel">
        <div className="cart-panel__header">
          <h2 className="cart-panel__title">🛒 Giỏ hàng</h2>
          <button
            id="cart-close-btn"
            className="btn btn--ghost btn--sm"
            onClick={() => setIsOpen(false)}
          >
            ✕
          </button>
        </div>

        {items.length === 0 ? (
          <div className="cart-panel__empty">
            <span style={{ fontSize: '3rem' }}>🛒</span>
            <p>Giỏ hàng trống</p>
            <button className="btn btn--ghost btn--sm" onClick={() => setIsOpen(false)}>
              Tiếp tục mua sắm
            </button>
          </div>
        ) : (
          <>
            <div className="cart-panel__items">
              {items.map((item) => (
                <div key={item.productId} className="cart-item">
                  <span className="cart-item__emoji">{item.emoji}</span>
                  <div className="cart-item__info">
                    <div className="cart-item__name">{item.productName}</div>
                    <div className="cart-item__price">
                      {item.price.toLocaleString()} {item.currency} / cái
                    </div>
                  </div>
                  <div className="cart-item__qty">
                    <button
                      className="qty-btn"
                      onClick={() => updateQty(item.productId, -1)}
                      id={`qty-dec-${item.productId}`}
                    >
                      −
                    </button>
                    <span className="qty-value">{item.quantity}</span>
                    <button
                      className="qty-btn"
                      onClick={() => updateQty(item.productId, 1)}
                      id={`qty-inc-${item.productId}`}
                    >
                      +
                    </button>
                    <button
                      className="qty-btn"
                      style={{ color: 'var(--color-danger)', marginLeft: 4 }}
                      onClick={() => removeItem(item.productId)}
                      id={`remove-${item.productId}`}
                    >
                      🗑
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="cart-panel__footer">
              {/* Payment Method Selector */}
              <div style={{ marginBottom: 14 }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-2)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span>💳</span> Chọn phương thức thanh toán:
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {[
                    { id: 'CARD', icon: '⚡', label: 'Cổng thanh toán tự động (Kafka Saga)', desc: 'Xử lý realtime & idempotent ngay' },
                    { id: 'EMAIL', icon: '📧', label: 'Thanh toán qua Email (Dev Mail)', desc: 'Mở Mailpit, bấm Accept để xác nhận thanh toán' },
                    { id: 'COD', icon: '💵', label: 'Thanh toán khi nhận hàng (COD)', desc: 'Tiền mặt khi giao hàng' },
                  ].map((m) => (
                    <label
                      key={m.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 10,
                        padding: '8px 10px',
                        borderRadius: 'var(--radius-sm)',
                        background: paymentMethod === m.id ? 'rgba(99, 102, 241, 0.12)' : 'var(--color-surface)',
                        border: `1px solid ${paymentMethod === m.id ? 'var(--color-primary)' : 'var(--color-border)'}`,
                        cursor: 'pointer',
                        fontSize: '0.8rem',
                        transition: 'border-color 0.2s',
                      }}
                    >
                      <input
                        type="radio"
                        name="cartPaymentMethod"
                        value={m.id}
                        checked={paymentMethod === m.id}
                        onChange={() => setPaymentMethod(m.id)}
                        style={{ accentColor: 'var(--color-primary)' }}
                      />
                      <div>
                        <div style={{ fontWeight: 600, color: 'var(--color-text)' }}>
                          {m.icon} {m.label}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--color-text-3)' }}>{m.desc}</div>
                      </div>
                    </label>
                  ))}
                </div>
              </div>

              {error && (
                <div className="alert alert--error" style={{ fontSize: '0.8rem', marginBottom: 12 }}>
                  ⚠️ {error}
                </div>
              )}
              <div className="cart-total">
                <span className="cart-total__label">Tổng cộng</span>
                <span className="cart-total__value">
                  {total.toLocaleString()} {currency}
                </span>
              </div>
              <button
                id="checkout-btn"
                className="btn btn--primary btn--full btn--lg"
                onClick={handleCheckout}
                disabled={placing}
              >
                {placing ? (
                  <>
                    <span className="spinner" />
                    Đang xử lý thanh toán...
                  </>
                ) : (
                  '💳 Xác nhận & Thanh toán ngay'
                )}
              </button>
              {!user && (
                <p style={{ textAlign: 'center', fontSize: '0.78rem', color: 'var(--color-text-3)', marginTop: 8 }}>
                  Bạn cần đăng nhập để thanh toán
                </p>
              )}
            </div>
          </>
        )}
      </aside>
    </>
  )
}
