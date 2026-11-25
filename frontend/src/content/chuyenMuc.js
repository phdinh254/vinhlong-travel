/** Chuyên mục bài viết, dùng chung cho trang Tin tức, chi tiết bài và form quản trị */
export const CHUYEN_MUC = [
  { key: 'NEWS', label: 'Tin tức', desc: 'Tin mới về tour và du lịch địa phương' },
  { key: 'CULTURE', label: 'Câu chuyện văn hóa', desc: 'Con người, lễ hội và di sản' },
  { key: 'EVENT', label: 'Sự kiện', desc: 'Lễ hội và hoạt động trong năm' },
  { key: 'TIPS', label: 'Kinh nghiệm', desc: 'Mẹo đi, ăn, ở cho chuyến đi' },
]

export const tenChuyenMuc = (key) => CHUYEN_MUC.find((c) => c.key === key)?.label || 'Tin tức'

/** Ảnh minh họa khi bài viết chưa có ảnh bìa (chọn theo chuyên mục + id để các thẻ không trùng nhau) */
const ANH_MAC_DINH = {
  NEWS: ['con-phung', 'song-nuoc', 'mang-thit'],
  CULTURE: ['chua-ang', 'lang-gom', 'van-thanh-mieu', 'chua-hang'],
  EVENT: ['chua-hang', 'vui-choi', 'ao-ba-om'],
  TIPS: ['an-binh', 'keo-dua', 'homestay', 'ao-ba-om'],
}
export const anhMacDinh = (n) => {
  const list = ANH_MAC_DINH[n.category] || ANH_MAC_DINH.NEWS
  return `/images/${list[(n.id || 0) % list.length]}.jpg`
}

/** Ước lượng thời gian đọc từ nội dung HTML (~200 từ/phút) */
export const phutDoc = (html) => {
  const words = (html || '').replace(/<[^>]+>/g, ' ').trim().split(/\s+/).filter(Boolean).length
  return Math.max(2, Math.round(words / 200))
}
