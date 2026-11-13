import { Link } from 'react-router-dom'
import { FOOTER_EXPLORE_MENU, POLICY_MENU, SOCIAL_KEYS, phoneDigits } from '../menus.js'

const YEAR = new Date().getFullYear()

/* ---------- Icon SVG (dùng currentColor để ăn theo màu chữ) ---------- */
const I = {
  pin: <path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21Zm0-9a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z" />,
  phone: <path d="M5 4h3l2 5-2.5 1.5a11 11 0 0 0 6 6L15 14l5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2Z" />,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
}
const Icon = ({ name }) => (
  <svg className="foot-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{I[name]}</svg>
)

const SOCIAL_ICON = {
  FACEBOOK: <path d="M14 8h3V4h-3a4 4 0 0 0-4 4v2H7v4h3v8h4v-8h3l1-4h-4V8.5a.5.5 0 0 1 .5-.5Z" />,
  YOUTUBE: <path d="M21.6 7.2a2.5 2.5 0 0 0-1.8-1.8C18.2 5 12 5 12 5s-6.2 0-7.8.4A2.5 2.5 0 0 0 2.4 7.2 26 26 0 0 0 2 12a26 26 0 0 0 .4 4.8 2.5 2.5 0 0 0 1.8 1.8C5.8 19 12 19 12 19s6.2 0 7.8-.4a2.5 2.5 0 0 0 1.8-1.8A26 26 0 0 0 22 12a26 26 0 0 0-.4-4.8ZM10 15V9l5.2 3L10 15Z" />,
  TIKTOK: <path d="M16.5 3a4.5 4.5 0 0 0 4.5 4.5V11a8 8 0 0 1-4.5-1.4V15a6 6 0 1 1-6-6h.5v3.6a2.5 2.5 0 1 0 2 2.4V3h3.5Z" />,
  INSTAGRAM: <path d="M7.5 3h9A4.5 4.5 0 0 1 21 7.5v9a4.5 4.5 0 0 1-4.5 4.5h-9A4.5 4.5 0 0 1 3 16.5v-9A4.5 4.5 0 0 1 7.5 3Zm4.5 5a4 4 0 1 0 0 8 4 4 0 0 0 0-8Zm0 2a2 2 0 1 1 0 4 2 2 0 0 1 0-4Zm5-3.2a1.2 1.2 0 1 0 0 2.4 1.2 1.2 0 0 0 0-2.4Z" fillRule="evenodd" />,
  ZALO: <path d="M12 3C6.5 3 2 6.9 2 11.7c0 2.7 1.4 5.1 3.7 6.7L5 21.5l3.6-1.8c1.1.3 2.2.5 3.4.5 5.5 0 10-3.9 10-8.6S17.5 3 12 3Zm-4.8 6.3h3.6l-2.6 3.5h2.6V14H6.9l2.6-3.5H7.2V9.3Zm5.1 0h1.2V14h-1.2V9.3Zm2.4 0h1.2v3.5h1.9V14h-3.1V9.3Z" fillRule="evenodd" />,
}

/* ---------- Thành phần ---------- */

function FooterBrand() {
  return (
    <div className="foot-brand-row">
      <Link to="/" aria-label="Vĩnh Long Travel – Trang chủ"><img src="/logo-dark.svg" alt="Vĩnh Long Travel" height="48" /></Link>
      <p>Tour miệt vườn, xứ dừa và văn hóa Khmer — đi chậm, ở lâu, hiểu sâu miền Tây.</p>
    </div>
  )
}

/** Cột Khám phá: render từ FOOTER_EXPLORE_MENU (menus.js) */
function FooterExplore({ items }) {
  return (
    <nav className="foot-col" aria-label="Khám phá">
      <h4>Khám phá</h4>
      <ul className="foot-links">
        {items.map(([to, label]) => <li key={to}><Link to={to}>{label}</Link></li>)}
      </ul>
    </nav>
  )
}

/** Cột Liên hệ: dữ liệu admin nhập ở "Giới thiệu & thông tin" (bảng CauHinhTrang) */
function FooterContact({ cfg }) {
  const tel = phoneDigits(cfg.HOTLINE)
  return (
    <div className="foot-col">
      <h4>Liên hệ</h4>
      <ul className="foot-contact">
        {cfg.DIA_CHI && <li><Icon name="pin" /><span>{cfg.DIA_CHI}</span></li>}
        {tel && <li><Icon name="phone" /><a href={`tel:${tel}`}>{cfg.HOTLINE}</a></li>}
        {cfg.EMAIL && <li><Icon name="mail" /><a href={`mailto:${cfg.EMAIL}`}>{cfg.EMAIL}</a></li>}
        {cfg.GIO_LAM_VIEC && <li><Icon name="clock" /><span>{cfg.GIO_LAM_VIEC}</span></li>}
      </ul>
    </div>
  )
}

/** Cột Mạng xã hội: chỉ hiện kênh admin đã nhập link; Zalo tự sinh từ hotline */
function FooterSocial({ cfg }) {
  const tel = phoneDigits(cfg.HOTLINE)
  const links = SOCIAL_KEYS.filter(([k]) => cfg[k]).map(([k, label]) => [k, label, cfg[k]])
  if (tel) links.push(['ZALO', 'Zalo', `https://zalo.me/${tel}`])
  return (
    <div className="foot-col">
      <h4>Kết nối</h4>
      <p>Theo dõi để nhận ưu đãi và cảm hứng cho chuyến đi tiếp theo.</p>
      {links.length > 0 ? (
        <ul className="foot-social">
          {links.map(([k, label, href]) => (
            <li key={k}>
              <a href={href} target="_blank" rel="noreferrer" aria-label={label} title={label}>
                <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">{SOCIAL_ICON[k]}</svg>
              </a>
            </li>
          ))}
        </ul>
      ) : <p className="foot-empty">Đang cập nhật.</p>}
    </div>
  )
}

/** Footer Bottom: copyright + menu chính sách */
function FooterBottom() {
  return (
    <div className="container foot-bottom">
      <span>© {YEAR} Vĩnh Long Travel. Ảnh: <a href="/images/CREDITS.md" target="_blank" rel="noreferrer">Wikimedia Commons</a></span>
      <nav aria-label="Chính sách">
        <ul className="foot-policy">
          {POLICY_MENU.map(([slug, label]) => <li key={slug}><Link to={`/chinh-sach/${slug}`}>{label}</Link></li>)}
        </ul>
      </nav>
    </div>
  )
}

export default function SiteFooter({ cfg = {} }) {
  return (
    <footer className="site-footer">
      <div className="container">
        <FooterBrand />
        <div className="foot-grid">
          <FooterExplore items={FOOTER_EXPLORE_MENU} />
          <FooterContact cfg={cfg} />
          <FooterSocial cfg={cfg} />
        </div>
      </div>
      <FooterBottom />
    </footer>
  )
}
