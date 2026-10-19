import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function LoginPage() {
  const { login, register } = useAuth()
  const navigate = useNavigate()

  const [mode, setMode] = useState('login') // 'login' | 'register'
  const [form, setForm] = useState({ email: '', password: '', fullName: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      if (mode === 'login') {
        await login(form.email, form.password)
      } else {
        await register(form.email, form.password, form.fullName)
      }
      navigate('/')
    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.error ||
        (mode === 'login' ? 'Email hoặc mật khẩu không đúng' : 'Đăng ký thất bại')
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">⚡ ShopFlow</div>
        <p className="auth-tagline">Nền tảng mua sắm thế hệ mới</p>

        <h1 className="auth-title">
          {mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}
        </h1>

        {error && (
          <div className="alert alert--error mb-4">
            ⚠️ {error}
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} id="auth-form">
          {mode === 'register' && (
            <div className="form-group">
              <label className="form-label" htmlFor="fullName">Họ và tên</label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                className="form-input"
                placeholder="Nguyễn Văn A"
                value={form.fullName}
                onChange={handleChange}
                required
                autoComplete="name"
              />
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              className="form-input"
              placeholder="you@example.com"
              value={form.email}
              onChange={handleChange}
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">Mật khẩu</label>
            <input
              id="password"
              name="password"
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={form.password}
              onChange={handleChange}
              required
              minLength={8}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
          </div>

          <button
            id="auth-submit-btn"
            type="submit"
            className="btn btn--primary btn--full btn--lg"
            disabled={loading}
            style={{ marginTop: 8 }}
          >
            {loading ? (
              <>
                <span className="spinner" />
                {mode === 'login' ? 'Đang đăng nhập...' : 'Đang tạo tài khoản...'}
              </>
            ) : (
              mode === 'login' ? '🚀 Đăng nhập' : '✨ Tạo tài khoản'
            )}
          </button>
        </form>

        <div className="auth-switch">
          {mode === 'login' ? (
            <>
              Chưa có tài khoản?{' '}
              <a onClick={() => { setMode('register'); setError('') }}>
                Đăng ký ngay
              </a>
            </>
          ) : (
            <>
              Đã có tài khoản?{' '}
              <a onClick={() => { setMode('login'); setError('') }}>
                Đăng nhập
              </a>
            </>
          )}
        </div>

        {/* Quick fill for demo */}
        <div style={{
          marginTop: 24,
          padding: '12px 16px',
          background: 'var(--color-surface-2)',
          border: '1px dashed var(--color-border-2)',
          borderRadius: 'var(--radius-sm)',
          fontSize: '0.78rem',
          color: 'var(--color-text-3)',
        }}>
          💡 <strong style={{ color: 'var(--color-text-2)' }}>Demo:</strong>{' '}
          Đăng ký tài khoản mới với email bất kỳ, mật khẩu tối thiểu 8 ký tự.
        </div>
      </div>
    </div>
  )
}
