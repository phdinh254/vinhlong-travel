import { useEffect, useState } from 'react'
import { NavLink, Link, Outlet, useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../auth.jsx'

/** 5 mục chính của website */
const MENU = [
  ['/', 'Trang chủ'],
  ['/gioi-thieu', 'Giới thiệu'],
  ['/dich-vu', 'Dịch vụ'],
  ['/tin-tuc', 'Tin tức'],
  ['/lien-he', 'Liên hệ'],
]

export default function PublicLayout() {
  const { user, isAdmin, logout } = useAuth()
  const [cfg, setCfg] = useState({})
  const nav = useNavigate()

  useEffect(() => {
    api.get('/api/public/cau-hinh').then((r) => setCfg(r.data)).catch(() => {})
  }, [])

  return (
    <>
      <header className="site-header">
        <div className="container">
          <Link to="/" className="brand">🌴 Du lịch <span>Vĩnh Long</span></Link>
          <nav className="nav">
            {MENU.map(([to, label]) => (
              <NavLink key={to} to={to} end={to === '/'}>{label}</NavLink>
            ))}
          </nav>
          <div className="row">
            {user ? (
              <>
                {isAdmin && <Link className="btn sm sun" to="/admin">Quản trị</Link>}
                <Link to="/tai-khoan" className="small">👤 {user.hoTen}</Link>
                <button className="btn secondary sm" onClick={() => { logout(); nav('/') }}>Đăng xuất</button>
              </>
            ) : (
              <>
                <Link className="btn secondary sm" to="/dang-nhap">Đăng nhập</Link>
                <Link className="btn sm" to="/dang-ky">Đăng ký</Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main><Outlet context={cfg} /></main>

      <footer className="site-footer">
        <div className="container grid grid-3">
          <div>
            <h4>Du lịch Vĩnh Long</h4>
            <p className="small">Tour miệt vườn, xứ dừa và văn hóa Khmer trên địa bàn tỉnh Vĩnh Long (mới).</p>
          </div>
          <div>
            <h4>Liên hệ</h4>
            <p className="small">
              {cfg.DIA_CHI && <>📍 {cfg.DIA_CHI}<br /></>}
              {cfg.HOTLINE && <>📞 {cfg.HOTLINE}<br /></>}
              {cfg.EMAIL && <>✉️ {cfg.EMAIL}</>}
            </p>
          </div>
          <div>
            <h4>Liên kết</h4>
            <p className="small">{MENU.map(([to, l]) => <span key={to}><Link to={to}>{l}</Link><br /></span>)}</p>
          </div>
        </div>
        <div className="container small" style={{ marginTop: 20, opacity: .7 }}>© {new Date().getFullYear()} Du lịch Vĩnh Long</div>
      </footer>
    </>
  )
}
