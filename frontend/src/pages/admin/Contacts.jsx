import { useEffect, useState } from 'react'
import api, { errMsg, fmtDateTime, pageCount } from '../../api'
import { Alert, Loading, Pagination } from '../../components/ui.jsx'

export default function Contacts() {
  const [data, setData] = useState(null)
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [err, setErr] = useState('')
  const load = () => api.get('/api/v1/admin/contact-messages', { params: { status: status || undefined, page, size: 20 } })
    .then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  useEffect(() => { load() }, [status, page]) // eslint-disable-line react-hooks/exhaustive-deps
  const mark = async (l, s) => {
    try { await api.patch(`/api/v1/admin/contact-messages/${l.id}/status`, { status: s }); load() } catch (e) { setErr(errMsg(e)) }
  }
  return (
    <>
      <div className="admin-head"><h1>Liên hệ từ khách</h1>
        <select style={{ maxWidth: 200 }} value={status} onChange={(e) => { setPage(0); setStatus(e.target.value) }}>
          <option value="">Tất cả</option><option value="NEW">Mới</option><option value="HANDLED">Đã xử lý</option>
        </select>
      </div>
      <Alert>{err}</Alert>
      {!data ? <Loading /> : data.items.length === 0 ? <p className="muted">Chưa có liên hệ.</p> : (
        <>
          <div className="table-wrap"><table>
            <thead><tr><th>Người gửi</th><th>Tiêu đề / Nội dung</th><th>Thời gian</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>{data.items.map((l) => (
              <tr key={l.id} style={l.status === 'NEW' ? { fontWeight: 500 } : { opacity: .75 }}>
                <td>{l.fullName}<div className="small">{l.email && <a href={`mailto:${l.email}`}>{l.email}</a>}{l.phone && ` · ${l.phone}`}</div></td>
                <td><b>{l.subject}</b><div className="small" style={{ whiteSpace: 'pre-wrap' }}>{l.message}</div></td>
                <td className="small">{fmtDateTime(l.createdAt)}{l.handledBy && <div className="muted">Xử lý: {l.handledBy}</div>}</td>
                <td><span className={`badge ${l.status === 'NEW' ? 'warn' : 'ok'}`}>{l.status === 'NEW' ? 'Mới' : 'Đã xử lý'}</span></td>
                <td>{l.status === 'NEW'
                  ? <button className="btn sm" onClick={() => mark(l, 'HANDLED')}>Đã xử lý</button>
                  : <button className="btn link small" onClick={() => mark(l, 'NEW')}>Đánh dấu mới</button>}</td>
              </tr>
            ))}</tbody>
          </table></div>
          <Pagination page={page} totalPages={pageCount(data)} onChange={setPage} />
        </>
      )}
    </>
  )
}
