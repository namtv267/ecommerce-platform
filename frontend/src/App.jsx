import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { CartProvider } from './contexts/CartContext'
import Navbar from './components/Navbar'
import CartPanel from './components/CartPanel'
import LoginPage from './pages/LoginPage'
import ProductsPage from './pages/ProductsPage'
import OrderDetailPage from './pages/OrderDetailPage'
import OrdersPage from './pages/OrdersPage'

function RequireAuth({ children }) {
  const { user, loading } = useAuth()
  if (loading) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        gap: 16,
        color: 'var(--color-text-2)',
      }}>
        <span className="spinner spinner--lg" style={{ color: 'var(--color-primary)' }} />
        Đang tải...
      </div>
    )
  }
  return user ? children : <Navigate to="/login" replace />
}

function AppLayout() {
  const { loading } = useAuth()
  if (loading) return null

  return (
    <>
      <Navbar />
      <CartPanel />
      <main style={{ paddingBottom: 64 }}>
        <Routes>
          {/* Public */}
          <Route path="/" element={<ProductsPage />} />
          <Route path="/login" element={<LoginPage />} />

          {/* Protected */}
          <Route
            path="/orders"
            element={
              <RequireAuth>
                <OrdersPage />
              </RequireAuth>
            }
          />
          <Route
            path="/orders/:id"
            element={
              <RequireAuth>
                <OrderDetailPage />
              </RequireAuth>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <AppLayout />
      </CartProvider>
    </AuthProvider>
  )
}
