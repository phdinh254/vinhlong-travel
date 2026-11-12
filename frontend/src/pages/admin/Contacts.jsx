import { useEffect, useState } from 'react'
import api, { fmtDateTime } from '../../api'
import { Loading } from '../../components/ui.jsx'

export default function Contacts() {
  const [list, setList] = useState(null)
  const load = () => api.get('/api/admin/lien-he').then((r) => setList(r.data))
  useEffect(() => { load() }, [])
  const mark = async (l, trangThai) => { await api.put(`/api/admin/lien-he/${l.lienHeId}`, { trangThai }); load() }
  return (
    <>
      <div className="admin-head"><h1>Liên hệ từ khách</h1></div>
      {!list ? <Loading /> : list.length === 0 ? <p className="muted">Chưa có liên hệ.</p> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Người gửi</th><th>Tiêu đề / Nội dung</th><th>Thời gian</th><th>Trạng thái</th><th></th></tr></thead>
          <tbody>{list.map((l) => (
            <tr key={l.lienHeId} style={l.trangThai === 'MOI' ? { fontWeight: 500 } : { opacity: .75 }}>
              <td>{l.hoTen}<div className="small"><a href={`mailto:${l.email}`}>{l.email}</a>{l.soDienThoai && ` · ${l.soDienThoai}`}</div></td>
              <td><b>{l.tieuDe}</b><div className="small" style={{ whiteSpace: 'pre-wrap' }}>{l.noiDung}</div></td>
              <td className="small">{fmtDateTime(l.ngayGui)}</td>
              <td><span className={`badge ${l.trangThai === 'MOI' ? 'warn' : 'ok'}`}>{l.trangThai === 'MOI' ? 'Mới' : 'Đã xử lý'}</span></td>
              <td>{l.trangThai === 'MOI' ? <button className="btn sm" onClick={() => mark(l, 'DA_XU_LY')}>Đã xử lý</button> : <button className="btn link small" onClick={() => mark(l, 'MOI')}>Đánh dấu mới</button>}</td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
    </>
  )
}
