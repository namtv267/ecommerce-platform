import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// Attach JWT access token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Auto-refresh on 401
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        const refreshToken = localStorage.getItem('refresh_token')
        if (!refreshToken) throw new Error('No refresh token')
        const { data } = await axios.post('/api/auth/refresh', { refreshToken })
        localStorage.setItem('access_token', data.accessToken)
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return api(original)
      } catch {
        localStorage.clear()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// ── Auth ──────────────────────────────────────────────
export const authApi = {
  register: (email, password, fullName) =>
    api.post('/auth/register', { email, password, fullName }),

  login: (email, password) =>
    api.post('/auth/login', { email, password }),

  logout: (refreshToken) =>
    api.post('/auth/logout', { refreshToken }),

  me: () => api.get('/auth/me'),
}

// ── Products ──────────────────────────────────────────
export const productApi = {
  list: (page = 0, size = 20) =>
    api.get('/products', { params: { page, size } }),

  get: (id) => api.get(`/products/${id}`),
}

// ── Orders ────────────────────────────────────────────
export const orderApi = {
  /**
   * POST /api/orders
   * items: [{ productId, quantity }]
   * Idempotency-Key header prevents duplicate orders on retry
   */
  place: (items) => {
    const idempotencyKey = crypto.randomUUID()
    return api.post(
      '/orders',
      { items },
      { headers: { 'Idempotency-Key': idempotencyKey } }
    )
  },

  get: (id) => api.get(`/orders/${id}`),
}

export default api
