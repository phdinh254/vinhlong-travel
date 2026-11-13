import { NavLink, Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  return (
    <div className="admin">
      <aside>
        <Link to="/admin" className="brand"><img src="/logo-dark.svg" alt="Vĩnh Long Travel" height="40" /></Link>
        <NavLink to="/admin" end>Tổng quan</NavLink>
        <div className="group">Dịch vụ (Tour)</div>
        <NavLink to="/admin/tours">Tour & đợt khởi hành</NavLink>
        <NavLink to="/admin/don-dat">Đơn đặt tour</NavLink>
        <NavLink to="/admin/yeu-cau-huy">Yêu cầu hủy</NavLink>
        <NavLink to="/admin/danh-muc">Danh mục</NavLink>
        <div className="group">Nội dung</div>
        <NavLink to="/admin/gioi-thieu">Giới thiệu & thông tin</NavLink>
        <NavLink to="/admin/tin-tuc">Tin tức</NavLink>
        <NavLink to="/admin/lien-he">Liên hệ</NavLink>
        <div className="group">Hệ thống</div>
        <NavLink to="/admin/nguoi-dung">Người dùng</NavLink>
        <a href="/" target="_blank" rel="noreferrer">Xem website ↗</a>
        <div className="small" style={{ padding: '18px 12px 0', opacity: .8 }}>{user?.email}</div>
        <button className="btn secondary sm" style={{ margin: '8px 12px' }} onClick={() => { logout(); nav('/') }}>Đăng xuất</button>
      </aside>
      <main><Outlet /></main>
    </div>
  )
}
