import { useEffect, useState } from 'react'
import { NavLink, Link, Outlet, useNavigate } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../auth.jsx'
import SiteFooter from '../components/SiteFooter.jsx'
import { MAIN_MENU as MENU } from '../menus.js'

export default function PublicLayout() {
  const { user, isAdmin, logout } = useAuth()
  const [cfg, setCfg] = useState({})
  const nav = useNavigate()

  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    api.get('/api/public/cau-hinh').then((r) => setCfg(r.data)).catch(() => {})
  }, [])

  // Header nền mờ (glass) khi cuộn trang
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <>
      <header className={`site-header ${scrolled ? 'scrolled' : ''}`}>
        <div className="container">
          <Link to="/" className="brand" aria-label="Vĩnh Long Travel – Trang chủ"><img src="/logo.svg" alt="Vĩnh Long Travel" height="44" /></Link>
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
              <Link className="head-login small" to="/dang-nhap">Đăng nhập</Link>
            )}
            <Link className="btn sm sun head-cta" to="/tour">Đặt tour ngay</Link>
          </div>
        </div>
      </header>

      <main><Outlet context={cfg} /></main>

      <SiteFooter cfg={cfg} />
    </>
  )
}
