import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import { paymentApi } from '../api/client'

export default function PaymentConfirmPage() {
  const [params] = useSearchParams()
  const orderId = params.get('orderId')
  const navigate = useNavigate()

  const [status, setStatus] = useState('processing') // processing | success | error
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    if (!orderId) {
      setStatus('error')
      setErrorMsg('Không tìm thấy mã đơn hàng trong đường link.')
      return
    }

    const approvePayment = async () => {
      try {
        await paymentApi.approve(orderId)
        setStatus('success')
        // Automatically redirect to order detail page after 2.5 seconds
        setTimeout(() => {
          navigate(`/orders/${orderId}`)
        }, 2500)
      } catch (err) {
        // Fallback for mock orders
        const mockOrders = JSON.parse(localStorage.getItem('mock_orders_data') || '{}')
        if (mockOrders[orderId]) {
          mockOrders[orderId].status = 'PAID'
          localStorage.setItem('mock_orders_data', JSON.stringify(mockOrders))
          setStatus('success')
          setTimeout(() => {
            navigate(`/orders/${orderId}`)
          }, 2500)
        } else {
          setStatus('error')
          setErrorMsg(err.response?.data?.message || err.message || 'Duyệt thanh toán thất bại')
        }
      }
    }

    approvePayment()
  }, [orderId, navigate])

  return (
    <div className="container" style={{ maxWidth: 540, padding: '80px 24px', textAlign: 'center' }}>
      <div style={{
        background: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        padding: '48px 32px',
        boxShadow: 'var(--shadow-lg)'
      }}>
        {status === 'processing' && (
          <>
            <span className="spinner spinner--lg" style={{ color: 'var(--color-primary)', margin: '0 auto 20px', display: 'block' }} />
            <h2 style={{ fontSize: '1.4rem', marginBottom: 8 }}>Đang xác nhận thanh toán...</h2>
            <p style={{ color: 'var(--color-text-3)', fontSize: '0.9rem' }}>
              Đang gửi tín hiệu chấp thuận tới Kafka Saga và hoàn tất giao dịch cho đơn hàng <strong>#{orderId}</strong>.
            </p>
          </>
        )}

        {status === 'success' && (
          <>
            <div style={{ fontSize: '4rem', marginBottom: 16 }}>🎉</div>
            <h2 style={{ fontSize: '1.6rem', color: 'var(--color-success)', marginBottom: 12 }}>
              Thanh toán thành công!
            </h2>
            <p style={{ color: 'var(--color-text-2)', fontSize: '0.95rem', marginBottom: 24, lineHeight: 1.6 }}>
              Giao dịch đã được duyệt thành công qua <strong>Dev Mail (Mailpit)</strong>.<br />
              Đơn hàng của bạn đã chuyển sang trạng thái <strong>PAID ✅</strong>.
            </p>
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-3)', marginBottom: 24 }}>
              Đang tự động chuyển hướng về trang chi tiết đơn hàng trong giây lát...
            </div>
            <Link to={`/orders/${orderId}`}>
              <button className="btn btn--primary btn--full" id="view-order-btn">
                Xem chi tiết đơn hàng ngay →
              </button>
            </Link>
          </>
        )}

        {status === 'error' && (
          <>
            <div style={{ fontSize: '4rem', marginBottom: 16 }}>⚠️</div>
            <h2 style={{ fontSize: '1.4rem', color: 'var(--color-danger)', marginBottom: 12 }}>
              Xác nhận thất bại
            </h2>
            <p style={{ color: 'var(--color-danger)', fontSize: '0.9rem', marginBottom: 24 }}>
              {errorMsg}
            </p>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
              <Link to="/">
                <button className="btn btn--ghost">Về trang chủ</button>
              </Link>
              {orderId && (
                <Link to={`/orders/${orderId}`}>
                  <button className="btn btn--primary">Về đơn hàng</button>
                </Link>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
