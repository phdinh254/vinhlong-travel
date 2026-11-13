import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime, TRANG_THAI_DON } from '../../api'
import { useAuth } from '../../auth.jsx'
import { Alert, Badge, Loading } from '../../components/ui.jsx'

export default function Account() {
  const { user, updateUser } = useAuth()
  const [tab, setTab] = useState('don')
  const [dons, setDons] = useState(null)
  const [p, setP] = useState({ hoTen: user.hoTen, soDienThoai: user.soDienThoai || '', diaChi: user.diaChi || '' })
  const [pw, setPw] = useState({ matKhauCu: '', matKhauMoi: '' })
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  useEffect(() => { api.get('/api/me/bookings').then((r) => setDons(r.data)).catch(() => setDons([])) }, [])

  const save = async (e) => {
    e.preventDefault(); setMsg(''); setErr('')
    try { updateUser((await api.put('/api/me', p)).data); setMsg('Đã cập nhật hồ sơ') } catch (x) { setErr(errMsg(x)) }
  }
  const changePw = async (e) => {
    e.preventDefault(); setMsg(''); setErr('')
    try { await api.put('/api/me/mat-khau', pw); setPw({ matKhauCu: '', matKhauMoi: '' }); setMsg('Đã đổi mật khẩu') } catch (x) { setErr(errMsg(x)) }
  }

  return (
    <div className="container section">
      <h1>Tài khoản của tôi</h1>
      <div className="tabs">
        <button className={tab === 'don' ? 'active' : ''} onClick={() => setTab('don')}>Đơn đặt tour</button>
        <button className={tab === 'ho-so' ? 'active' : ''} onClick={() => setTab('ho-so')}>Hồ sơ</button>
      </div>
      {tab === 'don' && (!dons ? <Loading /> : dons.length === 0 ? <p className="muted">Bạn chưa có đơn nào. <Link to="/tour">Xem tour</Link></p> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Mã đơn</th><th>Tour</th><th>Khởi hành</th><th>Số khách</th><th className="right">Tổng tiền</th><th className="right">Đã trả</th><th>Trạng thái</th></tr></thead>
          <tbody>{dons.map((d) => (
            <tr key={d.donDatTourId}><td><Link to={`/don/${d.maDon}`}>{d.maDon}</Link></td><td>{d.tenTour}</td><td>{fmtDateTime(d.ngayGioDi)}</td>
              <td>{d.soHanhKhach}</td><td className="right">{money(d.tongTien)}</td><td className="right">{money(d.daThanhToan)}</td><td><Badge map={TRANG_THAI_DON} value={d.trangThai} /></td></tr>
          ))}</tbody>
        </table></div>
      ))}
      {tab === 'ho-so' && (
        <div className="grid grid-2">
          <form className="card" onSubmit={save}>
            <h3>Thông tin cá nhân</h3>
            <p className="muted small">{user.email} · Đăng nhập bằng {user.nhaCungCap === 'GOOGLE' ? 'Google' : 'email'}</p>
            <div className="field"><label>Họ tên</label><input required value={p.hoTen} onChange={(e) => setP({ ...p, hoTen: e.target.value })} /></div>
            <div className="field"><label>Số điện thoại</label><input value={p.soDienThoai} onChange={(e) => setP({ ...p, soDienThoai: e.target.value })} /></div>
            <div className="field"><label>Địa chỉ</label><input value={p.diaChi} onChange={(e) => setP({ ...p, diaChi: e.target.value })} /></div>
            <button className="btn">Lưu</button>
          </form>
          <form className="card" onSubmit={changePw}>
            <h3>{user.nhaCungCap === 'GOOGLE' ? 'Đặt mật khẩu' : 'Đổi mật khẩu'}</h3>
            {user.nhaCungCap !== 'GOOGLE' && <div className="field"><label>Mật khẩu hiện tại</label><input type="password" value={pw.matKhauCu} onChange={(e) => setPw({ ...pw, matKhauCu: e.target.value })} /></div>}
            <div className="field"><label>Mật khẩu mới</label><input type="password" minLength={6} required value={pw.matKhauMoi} onChange={(e) => setPw({ ...pw, matKhauMoi: e.target.value })} /></div>
            <button className="btn">Cập nhật</button>
          </form>
          <div style={{ gridColumn: '1 / -1' }}><Alert type="ok">{msg}</Alert><Alert>{err}</Alert></div>
        </div>
      )}
    </div>
  )
}
