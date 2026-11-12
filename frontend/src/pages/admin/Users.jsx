import { useEffect, useState } from 'react'
import api, { errMsg, fmtDateTime } from '../../api'
import { Alert, Loading } from '../../components/ui.jsx'

export default function Users() {
  const [list, setList] = useState(null)
  const [err, setErr] = useState('')
  const load = () => api.get('/api/admin/users').then((r) => setList(r.data))
  useEffect(() => { load() }, [])
  const setTT = async (u, trangThai) => {
    try { await api.put(`/api/admin/users/${u.taiKhoanId}/trang-thai`, { trangThai }); load() } catch (e) { setErr(errMsg(e)) }
  }
  return (
    <>
      <div className="admin-head"><h1>Người dùng</h1></div>
      <p className="muted small">Khách vãng lai không có tài khoản; lịch sử đặt của họ được lưu theo số điện thoại trong bảng Khách hàng.</p>
      <Alert>{err}</Alert>
      {!list ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Họ tên</th><th>Email</th><th>Vai trò</th><th>Đăng nhập bằng</th><th>Ngày tạo</th><th>Trạng thái</th><th></th></tr></thead>
          <tbody>{list.map((u) => (
            <tr key={u.taiKhoanId}><td>{u.hoTen}</td><td>{u.email}</td>
              <td><span className={`badge ${u.vaiTro === 'ADMIN' ? 'warn' : 'info'}`}>{u.vaiTro === 'ADMIN' ? 'Admin' : 'Khách hàng'}</span></td>
              <td>{u.nhaCungCap === 'GOOGLE' ? 'Google' : 'Email'}</td><td className="small">{fmtDateTime(u.ngayTao)}</td>
              <td><span className={`badge ${u.trangThai === 'KHOA' ? 'bad' : 'ok'}`}>{u.trangThai === 'KHOA' ? 'Đã khóa' : 'Hoạt động'}</span></td>
              <td>{u.trangThai === 'KHOA' ? <button className="btn secondary sm" onClick={() => setTT(u, 'HOAT_DONG')}>Mở khóa</button> : <button className="btn link small" onClick={() => setTT(u, 'KHOA')}>Khóa</button>}</td></tr>
          ))}</tbody>
        </table></div>
      )}
    </>
  )
}
