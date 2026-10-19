import { useEffect, useState } from 'react'
import { productApi } from '../api/client'
import { useCart } from '../contexts/CartContext'
import { getProductEmoji, formatPrice } from '../utils/helpers'
import { MOCK_PRODUCTS } from '../utils/mockData'

function ProductSkeleton() {
  return (
    <div className="product-card" style={{ pointerEvents: 'none' }}>
      <div className="product-card__image skeleton" style={{ height: 180 }} />
      <div className="product-card__body" style={{ gap: 10 }}>
        <div className="skeleton" style={{ height: 12, width: '40%' }} />
        <div className="skeleton" style={{ height: 16, width: '80%' }} />
        <div className="skeleton" style={{ height: 12, width: '60%' }} />
        <div className="skeleton" style={{ height: 12, width: '50%' }} />
      </div>
      <div className="product-card__footer">
        <div className="skeleton" style={{ height: 20, width: '45%' }} />
        <div className="skeleton" style={{ height: 34, width: 80, borderRadius: 6 }} />
      </div>
    </div>
  )
}

export default function ProductsPage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [isMock, setIsMock] = useState(false)
  const [error, setError] = useState('')
  const [addedId, setAddedId] = useState(null)
  const { addItem } = useCart()

  const loadProducts = () => {
    setLoading(true)
    setError('')
    productApi
      .list(0, 20)
      .then(({ data }) => {
        if (Array.isArray(data) && data.length > 0) {
          setProducts(data)
          setIsMock(false)
        } else {
          setProducts(MOCK_PRODUCTS)
          setIsMock(true)
        }
      })
      .catch(() => {
        setProducts(MOCK_PRODUCTS)
        setIsMock(true)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadProducts()
  }, [])

  const handleAdd = (product) => {
    addItem({ ...product, emoji: getProductEmoji(product.name, product.sku) })
    setAddedId(product.id)
    setTimeout(() => setAddedId(null), 1200)
  }

  return (
    <div className="container">
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title">🛍️ Sản phẩm</h1>
          <p className="page-subtitle">Khám phá các sản phẩm công nghệ chất lượng cao</p>
        </div>

        {isMock && (
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 10,
            background: 'rgba(56, 189, 248, 0.1)',
            border: '1px solid rgba(56, 189, 248, 0.3)',
            borderRadius: 'var(--radius-md)',
            padding: '8px 14px',
            fontSize: '0.85rem',
            color: 'var(--color-primary)',
          }}>
            <span>✨ Chế độ Mock Data (Sẵn sàng trải nghiệm)</span>
            <button
              onClick={loadProducts}
              className="btn btn--ghost btn--sm"
              style={{ padding: '4px 8px', fontSize: '0.75rem' }}
              title="Thử kết nối lại với Backend"
            >
              🔄 Kết nối Backend
            </button>
          </div>
        )}
      </div>

      <div className="product-grid">
        {loading
          ? Array.from({ length: 8 }).map((_, i) => <ProductSkeleton key={i} />)
          : products.map((p) => (
              <ProductCard
                key={p.id}
                product={p}
                onAdd={handleAdd}
                justAdded={addedId === p.id}
              />
            ))}
      </div>
    </div>
  )
}

function ProductCard({ product, onAdd, justAdded }) {
  const emoji = getProductEmoji(product.name, product.sku)
  const isOutOfStock = product.stock === 0

  return (
    <div
      className="product-card"
      id={`product-card-${product.id}`}
      style={justAdded ? { borderColor: 'var(--color-success)' } : {}}
    >
      <div className="product-card__image">
        <span style={{ fontSize: '4rem', zIndex: 1, position: 'relative' }}>{emoji}</span>
      </div>

      <div className="product-card__body">
        <span className="product-card__sku">{product.sku}</span>
        <h3 className="product-card__name">{product.name}</h3>
        {product.description && (
          <p className="product-card__desc">{product.description}</p>
        )}
        <span className="product-stock">
          {isOutOfStock
            ? '❌ Hết hàng'
            : `✅ Còn ${product.stock} sản phẩm`}
        </span>
      </div>

      <div className="product-card__footer">
        <span className="product-price">{formatPrice(product.price, product.currency)}</span>
        <button
          id={`add-to-cart-${product.id}`}
          className="btn btn--primary btn--sm"
          onClick={() => onAdd(product)}
          disabled={isOutOfStock}
          style={justAdded
            ? { background: 'var(--color-success)', boxShadow: 'none' }
            : {}}
        >
          {justAdded ? '✓ Đã thêm' : '+ Thêm'}
        </button>
      </div>
    </div>
  )
}
