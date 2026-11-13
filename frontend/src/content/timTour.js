/** Lựa chọn thời lượng cho bộ lọc tour (giá trị gửi lên API: soNgay; 3 = từ 3 ngày trở lên) */
export const SO_NGAY = [
  { id: '1', ten: '1 ngày' },
  { id: '2', ten: '2 ngày 1 đêm' },
  { id: '3', ten: 'Từ 3 ngày' },
]

/** Ngân sách (lọc trên trình duyệt theo "giá từ" của tour) */
export const NGAN_SACH = [
  { id: 'duoi-500', ten: 'Dưới 500.000 đ', min: 0, max: 500000 },
  { id: '500-1tr', ten: '500.000 – 1.000.000 đ', min: 500000, max: 1000000 },
  { id: 'tren-1tr', ten: 'Trên 1.000.000 đ', min: 1000000, max: Infinity },
]

/** Sắp xếp (trên trình duyệt) */
export const SAP_XEP = [
  { id: '', ten: 'Nổi bật' },
  { id: 'gia-tang', ten: 'Giá thấp đến cao' },
  { id: 'gia-giam', ten: 'Giá cao đến thấp' },
  { id: 'khoi-hanh', ten: 'Khởi hành sớm nhất' },
  { id: 'ngan-ngay', ten: 'Thời lượng ngắn nhất' },
]

/** Ảnh đại diện cho ô "Khám phá theo khu vực" (so khớp theo tên khu vực trong DB) */
export const ANH_KHU_VUC = [
  ['Vĩnh Long', 'an-binh', 'Cù lao, miệt vườn, làng gốm'],
  ['Bến Tre', 'con-phung', 'Xứ dừa, cồn bãi, homestay'],
  ['Trà Vinh', 'chua-ang', 'Văn hóa Khmer, biển Ba Động'],
]
