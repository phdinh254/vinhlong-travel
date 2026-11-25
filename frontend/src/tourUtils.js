import { imgUrl } from './api'

/** Ảnh minh họa theo khu vực khi tour chưa có ảnh đại diện (không để khung ảnh trống) */
const ANH_KHU_VUC = [['Bến Tre', 'con-phung'], ['Trà Vinh', 'chua-ang'], ['Vĩnh Long', 'an-binh']]
export const tourImage = (t) => {
  const cover = t.coverImageUrl || (t.images || []).find((i) => i.cover)?.url || t.images?.[0]?.url
  if (cover) return imgUrl(cover)
  const hit = ANH_KHU_VUC.find(([k]) => (t.regions || []).some((x) => x.name.includes(k)))
  return `/images/${hit ? hit[1] : 'song-nuoc'}.jpg`
}
export const tenKhuVuc = (s) => s.replace(/^Khu vực\s*/i, '').replace(/\s*\(cũ\)\s*$/i, '')

/** "3 ngày 2 đêm" từ durationDays/durationNights */
export const thoiLuong = (t) => `${t.durationDays} ngày${t.durationNights ? ` ${t.durationNights} đêm` : ''}`

/** Mô tả một mốc phạt hủy: "Từ 15 ngày trở lên" / "3 – 6 ngày" */
export const tenMocPhat = (m) => (m.daysBeforeTo >= 365 ? `Từ ${m.daysBeforeFrom} ngày trở lên` : `${m.daysBeforeFrom} – ${m.daysBeforeTo} ngày`)
