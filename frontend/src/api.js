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

export const errMsg = (e) => e?.response?.data?.message || e?.message || 'Đã có lỗi xảy ra'

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
  const { data } = await api.post('/api/admin/files', fd)
  return data.url
}

export const TRANG_THAI_DON = {
  CHO_THANH_TOAN: ['Chờ thanh toán', 'warn'],
  DA_COC: ['Đã đặt cọc', 'info'],
  DA_THANH_TOAN: ['Đã thanh toán', 'ok'],
  HOAN_THANH: ['Hoàn thành', 'ok'],
  DA_HUY: ['Đã hủy', 'bad'],
  HET_HAN: ['Hết hạn giữ chỗ', 'muted'],
}

export const TRANG_THAI_DOT = {
  MO_BAN: ['Mở bán', 'ok'],
  HET_CHO: ['Hết chỗ', 'bad'],
  DONG: ['Đóng', 'muted'],
  DA_KHOI_HANH: ['Đã khởi hành', 'info'],
  HUY: ['Hủy', 'bad'],
}

export const TRANG_THAI_TOUR = {
  NHAP: ['Nháp', 'muted'],
  DANG_BAN: ['Đang bán', 'ok'],
  NGUNG_BAN: ['Ngừng bán', 'bad'],
}

export const LOAI_HOAT_DONG = {
  DI_CHUYEN: 'Di chuyển',
  AN_UONG: 'Ăn uống',
  THAM_QUAN: 'Tham quan',
  NGHI_NGOI: 'Nghỉ ngơi',
  KHAC: 'Khác',
}

export const CACH_TINH_PHU_THU = {
  THEO_NGUOI: 'Theo người',
  THEO_DON: 'Theo đơn',
  THEO_SO_LUONG: 'Theo số lượng',
}

/** Chuyển Date -> 'YYYY-MM-DDTHH:mm' cho input datetime-local */
export const toLocalInput = (v) => (v ? String(v).slice(0, 16) : '')
