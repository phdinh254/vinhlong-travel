import { imgUrl } from './api'

/** Ảnh minh họa theo khu vực khi tour chưa có ảnh đại diện (không để khung ảnh trống) */
const ANH_KHU_VUC = [['Bến Tre', 'con-phung'], ['Trà Vinh', 'chua-ang'], ['Vĩnh Long', 'an-binh']]
export const tourImage = (t) => {
  if (t.anhDaiDien) return imgUrl(t.anhDaiDien)
  const hit = ANH_KHU_VUC.find(([k]) => (t.khuVucs || []).some((x) => x.includes(k)))
  return `/images/${hit ? hit[1] : 'song-nuoc'}.jpg`
}
export const tenKhuVuc = (s) => s.replace(/^Khu vực\s*/i, '').replace(/\s*\(cũ\)\s*$/i, '')
