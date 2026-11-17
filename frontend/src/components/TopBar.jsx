import { Link } from 'react-router-dom'
import { phoneDigits } from '../menus.js'
import { SOCIAL_ICON, socialLinks } from './social.jsx'

/* Icon nét mảnh 24×24 (currentColor) */
const I = {
  phone: <path d="M5 4h3l2 5-2.5 1.5a11 11 0 0 0 6 6L15 14l5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2Z" />,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></>,
}
const Icon = ({ name }) => (
  <svg className="tb-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{I[name]}</svg>
)

/**
 * Thanh top bar phía trên header: hotline · email · giờ làm việc | tra cứu đơn · mạng xã hội.
 * Dữ liệu lấy từ cấu hình trang (admin nhập ở "Giới thiệu & thông tin"); mục nào trống thì ẩn.
 */
export default function TopBar({ cfg = {} }) {
  const tel = phoneDigits(cfg.HOTLINE)
  const social = socialLinks(cfg)
  return (
    <div className="topbar">
      <div className="container topbar-in">
        <ul className="tb-info" aria-label="Thông tin liên hệ">
          {tel && <li><a href={`tel:${tel}`}><Icon name="phone" /><span className="tb-k">Hotline</span><b>{cfg.HOTLINE}</b></a></li>}
          {cfg.EMAIL && <li className="tb-hide-sm"><a href={`mailto:${cfg.EMAIL}`}><Icon name="mail" />{cfg.EMAIL}</a></li>}
          {cfg.GIO_LAM_VIEC && <li className="tb-hide-md"><Icon name="clock" />{cfg.GIO_LAM_VIEC}</li>}
        </ul>
        <div className="tb-right">
          <Link to="/tra-cuu" className="tb-link"><Icon name="search" />Tra cứu đơn</Link>
          {social.length > 0 && (
            <ul className="tb-social tb-hide-sm" aria-label="Mạng xã hội">
              {social.map(([k, label, href]) => (
                <li key={k}>
                  <a href={href} target="_blank" rel="noreferrer" aria-label={label} title={label}>
                    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">{SOCIAL_ICON[k]}</svg>
                  </a>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
