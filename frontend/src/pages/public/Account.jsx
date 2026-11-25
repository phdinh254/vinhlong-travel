import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime, TRANG_THAI_DON } from '../../api'
import { useAuth } from '../../auth.jsx'
import { Alert, Badge, Loading } from '../../components/ui.jsx'

export default function Account() {
  const { user, updateUser } = useAuth()
  const [tab, setTab] = useState('don')
  const [dons, setDons] = useState(null)
  const [trips, setTrips] = useState(null)
  const [p, setP] = useState({ fullName: user.fullName || '', phone: user.phone || '' })
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' })
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  useEffect(() => {
    api.get('/api/v1/me/bookings').then((r) => setDons(r.data)).catch(() => setDons([]))
    api.get('/api/v1/me/itineraries').then((r) => setTrips(r.data)).catch(() => setTrips([]))
    // Lấy lại hồ sơ mới nhất (hasPassword, số điện thoại...)
    api.get('/api/v1/me').then((r) => updateUser(r.data)).catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const save = async (e) => {
    e.preventDefault(); setMsg(''); setErr('')
    const body = { fullName: p.fullName, phone: p.phone || null }
    try { updateUser((await api.put('/api/v1/me', body)).data); setMsg('Đã cập nhật hồ sơ') } catch (x) { setErr(errMsg(x)) }
  }
  const changePw = async (e) => {
    e.preventDefault(); setMsg(''); setErr('')
    const body = user.hasPassword === false ? { newPassword: pw.newPassword } : pw
    try {
      await api.put('/api/v1/me/password', body)
      setPw({ currentPassword: '', newPassword: '' })
      updateUser({ ...user, hasPassword: true })
      setMsg('Đã đổi mật khẩu')
    } catch (x) { setErr(errMsg(x)) }
  }
  const delTrip = async (t) => {
    if (!confirm(`Xóa lịch trình "${t.title}"?`)) return
    try { await api.delete(`/api/v1/me/itineraries/${t.id}`); setTrips(trips.filter((x) => x.id !== t.id)) } catch (x) { setErr(errMsg(x)) }
  }

  return (
    <div className="container section">
      <h1>Tài khoản của tôi</h1>
      <div className="tabs">
        <button className={tab === 'don' ? 'active' : ''} onClick={() => setTab('don')}>Đơn đặt tour</button>
        <button className={tab === 'lich-trinh' ? 'active' : ''} onClick={() => setTab('lich-trinh')}>Lịch trình đã lưu</button>
        <button className={tab === 'ho-so' ? 'active' : ''} onClick={() => setTab('ho-so')}>Hồ sơ</button>
      </div>
      {tab === 'don' && (!dons ? <Loading /> : dons.length === 0 ? <p className="muted">Bạn chưa có đơn nào. <Link to="/tour">Xem tour</Link></p> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Mã đơn</th><th>Tour</th><th>Khởi hành</th><th>Số khách</th><th className="right">Tổng tiền</th><th className="right">Đã trả</th><th>Trạng thái</th></tr></thead>
          <tbody>{dons.map((d) => (
            <tr key={d.id}><td><Link to={`/don/${d.code}`}>{d.code}</Link></td><td>{d.tourName}</td><td>{fmtDateTime(d.departureAt)}</td>
              <td>{d.passengerCount}</td><td className="right">{money(d.totalAmount)}</td><td className="right">{money(d.paidAmount)}</td><td><Badge map={TRANG_THAI_DON} value={d.status} /></td></tr>
          ))}</tbody>
        </table></div>
      ))}
      {tab === 'lich-trinh' && (!trips ? <Loading /> : trips.length === 0 ? <p className="muted">Bạn chưa lưu lịch trình nào. <Link to="/goi-y-lich-trinh">Tạo lịch trình gợi ý</Link></p> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Tên lịch trình</th><th>Ngày đi</th><th>Số ngày</th><th>Lưu lúc</th><th></th></tr></thead>
          <tbody>{trips.map((t) => (
            <tr key={t.id}><td><Link to={`/goi-y-lich-trinh?trip=${t.id}`}>{t.title}</Link></td><td>{new Date(t.startDate).toLocaleDateString('vi-VN')}</td>
              <td>{t.days}</td><td className="small">{fmtDateTime(t.createdAt)}</td>
              <td><button className="btn link small" onClick={() => delTrip(t)}>Xóa</button></td></tr>
          ))}</tbody>
        </table></div>
      ))}
      {tab === 'ho-so' && (
        <div className="grid grid-2">
          <form className="card" onSubmit={save}>
            <h3>Thông tin cá nhân</h3>
            <p className="muted small">{user.email}</p>
            <div className="field"><label>Họ tên</label><input required value={p.fullName} onChange={(e) => setP({ ...p, fullName: e.target.value })} /></div>
            <div className="field"><label>Số điện thoại</label><input value={p.phone} onChange={(e) => setP({ ...p, phone: e.target.value })} /></div>
            <button className="btn">Lưu</button>
          </form>
          <form className="card" onSubmit={changePw}>
            <h3>{user.hasPassword === false ? 'Đặt mật khẩu' : 'Đổi mật khẩu'}</h3>
            {user.hasPassword !== false && <div className="field"><label>Mật khẩu hiện tại</label><input type="password" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} /></div>}
            <div className="field"><label>Mật khẩu mới (tối thiểu 8 ký tự)</label><input type="password" minLength={8} required value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} /></div>
            <button className="btn">Cập nhật</button>
          </form>
          <div style={{ gridColumn: '1 / -1' }}><Alert type="ok">{msg}</Alert><Alert>{err}</Alert></div>
        </div>
      )}
    </div>
  )
}
