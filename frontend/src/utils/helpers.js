// Product emoji mapping by keyword in name/sku
const EMOJI_MAP = [
  [/(laptop|macbook|notebook)/i, '💻'],
  [/(phone|iphone|android|mobile)/i, '📱'],
  [/(tablet|ipad)/i, '📟'],
  [/(headphone|earphone|airpod)/i, '🎧'],
  [/(watch|smartwatch)/i, '⌚'],
  [/(camera|photo)/i, '📷'],
  [/(tv|television|monitor|screen)/i, '🖥️'],
  [/(keyboard)/i, '⌨️'],
  [/(mouse)/i, '🖱️'],
  [/(speaker|audio|sound)/i, '🔊'],
  [/(game|console|gaming)/i, '🎮'],
  [/(shoe|sneaker)/i, '👟'],
  [/(bag|backpack)/i, '🎒'],
  [/(book)/i, '📚'],
  [/(coffee|drink)/i, '☕'],
  [/(food|eat)/i, '🍔'],
]

export function getProductEmoji(name = '', sku = '') {
  const text = `${name} ${sku}`.toLowerCase()
  for (const [regex, emoji] of EMOJI_MAP) {
    if (regex.test(text)) return emoji
  }
  return '📦'
}

export function formatPrice(price, currency = 'USD') {
  return new Intl.NumberFormat(currency === 'VND' ? 'vi-VN' : 'en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: currency === 'VND' ? 0 : 2,
    maximumFractionDigits: currency === 'VND' ? 0 : 2,
  }).format(price)
}

export function formatDate(iso) {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}

export function shortId(id = '') {
  return id.slice(0, 8).toUpperCase()
}
