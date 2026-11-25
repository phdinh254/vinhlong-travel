import { SOCIAL_KEYS, phoneDigits } from '../menus.js'

/** Icon mạng xã hội (fill = currentColor) — dùng chung cho footer và top bar */
export const SOCIAL_ICON = {
  FACEBOOK: <path d="M14 8h3V4h-3a4 4 0 0 0-4 4v2H7v4h3v8h4v-8h3l1-4h-4V8.5a.5.5 0 0 1 .5-.5Z" />,
  YOUTUBE: <path d="M21.6 7.2a2.5 2.5 0 0 0-1.8-1.8C18.2 5 12 5 12 5s-6.2 0-7.8.4A2.5 2.5 0 0 0 2.4 7.2 26 26 0 0 0 2 12a26 26 0 0 0 .4 4.8 2.5 2.5 0 0 0 1.8 1.8C5.8 19 12 19 12 19s6.2 0 7.8-.4a2.5 2.5 0 0 0 1.8-1.8A26 26 0 0 0 22 12a26 26 0 0 0-.4-4.8ZM10 15V9l5.2 3L10 15Z" />,
  TIKTOK: <path d="M16.5 3a4.5 4.5 0 0 0 4.5 4.5V11a8 8 0 0 1-4.5-1.4V15a6 6 0 1 1-6-6h.5v3.6a2.5 2.5 0 1 0 2 2.4V3h3.5Z" />,
  INSTAGRAM: <path d="M7.5 3h9A4.5 4.5 0 0 1 21 7.5v9a4.5 4.5 0 0 1-4.5 4.5h-9A4.5 4.5 0 0 1 3 16.5v-9A4.5 4.5 0 0 1 7.5 3Zm4.5 5a4 4 0 1 0 0 8 4 4 0 0 0 0-8Zm0 2a2 2 0 1 1 0 4 2 2 0 0 1 0-4Zm5-3.2a1.2 1.2 0 1 0 0 2.4 1.2 1.2 0 0 0 0-2.4Z" fillRule="evenodd" />,
  ZALO: <path d="M12 3C6.5 3 2 6.9 2 11.7c0 2.7 1.4 5.1 3.7 6.7L5 21.5l3.6-1.8c1.1.3 2.2.5 3.4.5 5.5 0 10-3.9 10-8.6S17.5 3 12 3Zm-4.8 6.3h3.6l-2.6 3.5h2.6V14H6.9l2.6-3.5H7.2V9.3Zm5.1 0h1.2V14h-1.2V9.3Zm2.4 0h1.2v3.5h1.9V14h-3.1V9.3Z" fillRule="evenodd" />,
}

/** Các kênh admin đã nhập link (bảng CauHinhTrang); Zalo tự sinh từ hotline. Trả về [key, nhãn, href] */
export function socialLinks(cfg) {
  const tel = phoneDigits(cfg.HOTLINE)
  const links = SOCIAL_KEYS.filter(([k]) => cfg[`${k}_URL`]).map(([k, label]) => [k, label, cfg[`${k}_URL`]])
  if (tel) links.push(['ZALO', 'Zalo', `https://zalo.me/${tel}`])
  return links
}
