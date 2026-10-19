import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useCart } from '../contexts/CartContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const { count, setIsOpen } = useCart()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <div className="container navbar__inner">
        <NavLink to="/" className="navbar__logo">⚡ ShopFlow</NavLink>

        <div className="navbar__links">
          <NavLink
            to="/"
            className={({ isActive }) =>
              `navbar__link${isActive ? ' navbar__link--active' : ''}`
            }
            end
          >
            🛍️ Sản phẩm
          </NavLink>

          {user && (
            <NavLink
              to="/orders"
              className={({ isActive }) =>
                `navbar__link${isActive ? ' navbar__link--active' : ''}`
              }
            >
              📋 Đơn hàng
            </NavLink>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {user ? (
            <>
              <button
                id="cart-toggle-btn"
                className="navbar__cart-btn"
                onClick={() => setIsOpen((o) => !o)}
              >
                🛒 Giỏ hàng
                {count > 0 && <span className="cart-badge">{count}</span>}
              </button>
              <span className="user-email">{user.email}</span>
              <button
                id="logout-btn"
                className="btn btn--ghost btn--sm"
                onClick={handleLogout}
              >
                Đăng xuất
              </button>
            </>
          ) : (
            <NavLink to="/login">
              <button id="login-nav-btn" className="btn btn--primary btn--sm">
                Đăng nhập
              </button>
            </NavLink>
          )}
        </div>
      </div>
    </nav>
  )
}
