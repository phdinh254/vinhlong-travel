import axios from 'axios'

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081'

const api = axios.create({ baseURL: API_URL })

api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401 && localStorage.getItem('token')) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
    return Promise.reject(err)
  },
)

export default api

export const errMsg = (e) => {
  const d = e?.response?.data
  const fields = d?.fields ? Object.values(d.fields) : []
  if (d?.message) return fields.length ? `${d.message}: ${fields.join('; ')}` : d.message
  return e?.message || 'Đã có lỗi xảy ra'
}

export const money = (v) =>
  v == null ? '—' : Number(v).toLocaleString('vi-VN', { maximumFractionDigits: 0 }) + ' đ'

export const fmtDate = (v) => (v ? new Date(v).toLocaleDateString('vi-VN') : '—')

export const fmtDateTime = (v) =>
  v ? new Date(v).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '—'

/** Ảnh tĩnh của frontend (/images/...) giữ nguyên; ảnh upload lấy từ backend */
export const imgUrl = (p) => (!p ? null : p.startsWith('http') || p.startsWith('/images/') ? p : API_URL + p)

export const uploadImage = async (file) => {
  const fd = new FormData()
  fd.append('file', file)
  const { data } = await api.post('/api/v1/admin/uploads', fd)
  return data.url
}

/** Tải các danh mục công khai dùng cho bộ lọc: { regions, categories, amenities, destinations, surcharges } (mỗi mục { id, name, ... }) */
export const loadCatalogs = async () => {
  const get = (url, params) => api.get(url, { params }).then((r) => r.data).catch(() => null)
  const [regions, categories, amenities, dest, surcharges] = await Promise.all([
    get('/api/v1/regions'), get('/api/v1/tour-categories'), get('/api/v1/amenities'),
    get('/api/v1/destinations', { size: 100 }), get('/api/v1/surcharges'),
  ])
  return {
    regions: regions || [], categories: categories || [], amenities: amenities || [],
    destinations: (dest?.items || []).sort((a, b) => a.name.localeCompare(b.name, 'vi')), surcharges: surcharges || [],
  }
}
export const EMPTY_CATALOGS = { regions: [], categories: [], amenities: [], destinations: [], surcharges: [] }

/** Bộ lọc thời lượng trên URL (soNgay = 1 | 2 | 3 nghĩa là từ 3 ngày) -> tham số API minDays/maxDays */
export const soNgayParams = (v) => (v === '3' ? { minDays: 3 } : v ? { minDays: Number(v), maxDays: Number(v) } : {})

/** Số trang của kết quả phân trang { items, page, size, total } */
export const pageCount = (d) => (d && d.size ? Math.ceil(d.total / d.size) : 0)

export const TRANG_THAI_DON = {
  PENDING_PAYMENT: ['Chờ thanh toán', 'warn'],
  DEPOSITED: ['Đã đặt cọc', 'info'],
  PAID: ['Đã thanh toán', 'ok'],
  COMPLETED: ['Hoàn thành', 'ok'],
  CANCELLED: ['Đã hủy', 'bad'],
  EXPIRED: ['Hết hạn giữ chỗ', 'muted'],
}

export const TRANG_THAI_DOT = {
  OPEN: ['Mở bán', 'ok'],
  CLOSED: ['Đóng', 'muted'],
  CANCELLED: ['Hủy', 'bad'],
}

/** Nhãn hiển thị cho một đợt: hết chỗ / quá hạn đặt được suy ra từ seatsLeft và bookable */
export const nhanDot = (d) =>
  d.status === 'OPEN' && d.seatsLeft === 0 ? ['Hết chỗ', 'bad']
    : d.status === 'OPEN' && !d.bookable ? ['Ngừng nhận đặt', 'muted']
      : TRANG_THAI_DOT[d.status] || [d.status, 'muted']

export const TRANG_THAI_TOUR = {
  DRAFT: ['Nháp', 'muted'],
  ON_SALE: ['Đang bán', 'ok'],
  STOPPED: ['Ngừng bán', 'bad'],
}

export const CACH_TINH_PHU_THU = {
  PER_PERSON: 'Theo người',
  PER_BOOKING: 'Theo đơn',
  PER_UNIT: 'Theo số lượng',
}

export const PHUONG_THUC_TT = { PAYOS: 'payOS', CASH: 'Tiền mặt', BANK_TRANSFER: 'Chuyển khoản' }
export const TRANG_THAI_TT = { PENDING: ['Đang chờ', 'warn'], SUCCEEDED: ['Thành công', 'ok'], FAILED: ['Thất bại', 'bad'] }
export const TRANG_THAI_YC_HUY = { PENDING: ['Chờ duyệt', 'warn'], APPROVED: ['Đã duyệt', 'ok'], REJECTED: ['Từ chối', 'bad'] }

/** Chuyển Date -> 'YYYY-MM-DDTHH:mm' cho input datetime-local */
export const toLocalInput = (v) => (v ? String(v).slice(0, 16) : '')

/** Giờ 'HH:mm:ss' -> 'HH:mm' */
export const hhmm = (v) => (v ? String(v).slice(0, 5) : '')
