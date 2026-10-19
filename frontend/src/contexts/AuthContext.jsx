import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { authApi } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)       // { userId, email, roles }
  const [loading, setLoading] = useState(true)

  // Restore session on mount
  useEffect(() => {
    const token = localStorage.getItem('access_token')
    if (!token) { setLoading(false); return }

    authApi.me()
      .then(({ data }) => setUser(data))
      .catch(() => { localStorage.clear(); setUser(null) })
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (email, password) => {
    const { data } = await authApi.login(email, password)
    localStorage.setItem('access_token', data.accessToken)
    localStorage.setItem('refresh_token', data.refreshToken)
    const me = await authApi.me()
    setUser(me.data)
  }, [])

  const register = useCallback(async (email, password, fullName) => {
    await authApi.register(email, password, fullName)
    // Auto-login after register
    await login(email, password)
  }, [login])

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem('refresh_token')
    try { if (refreshToken) await authApi.logout(refreshToken) } catch {}
    localStorage.clear()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
