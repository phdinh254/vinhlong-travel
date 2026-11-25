import { useEffect, useState } from 'react'
import api, { errMsg, fmtDateTime, pageCount } from '../../api'
import { Alert, Loading, Pagination } from '../../components/ui.jsx'

export default function Users() {
  const [data, setData] = useState(null)
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [err, setErr] = useState('')
  const load = (p = page) =>
    api.get('/api/v1/admin/accounts', { params: { q: q || undefined, page: p, size: 20 } })
      .then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  useEffect(() => { load(page) }, [page]) // eslint-disable-line react-hooks/exhaustive-deps
  const setStatus = async (u, status) => {
    setErr('')
    try { await api.patch(`/api/v1/admin/accounts/${u.id}/status`, { status }); load() } catch (e) { setErr(errMsg(e)) }
  }
  return (
    <>
      <div className="admin-head"><h1>Người dùng</h1></div>
      <p className="muted small">Khách vãng lai không cần tài khoản; đơn của họ được tra cứu bằng mã đơn và số điện thoại.</p>
      <form className="row" style={{ marginBottom: 12 }} onSubmit={(e) => { e.preventDefault(); setPage(0); load(0) }}>
        <input placeholder="Tìm theo email, họ tên, số điện thoại" value={q} onChange={(e) => setQ(e.target.value)} style={{ maxWidth: 320 }} />
        <button className="btn secondary sm">Tìm</button>
      </form>
      <Alert>{err}</Alert>
      {!data ? <Loading /> : (
        <>
          <div className="table-wrap"><table>
            <thead><tr><th>Họ tên</th><th>Email</th><th>Điện thoại</th><th>Vai trò</th><th>Ngày tạo</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>{data.items.map((u) => (
              <tr key={u.id}><td>{u.fullName}</td><td>{u.email}</td><td>{u.phone || '—'}</td>
                <td><span className={`badge ${u.role === 'ADMIN' ? 'warn' : 'info'}`}>{u.role === 'ADMIN' ? 'Admin' : 'Khách hàng'}</span></td>
                <td className="small">{fmtDateTime(u.createdAt)}</td>
                <td><span className={`badge ${u.status === 'LOCKED' ? 'bad' : 'ok'}`}>{u.status === 'LOCKED' ? 'Đã khóa' : 'Hoạt động'}</span></td>
                <td>{u.status === 'LOCKED'
                  ? <button className="btn secondary sm" onClick={() => setStatus(u, 'ACTIVE')}>Mở khóa</button>
                  : <button className="btn link small" onClick={() => setStatus(u, 'LOCKED')}>Khóa</button>}</td></tr>
            ))}</tbody>
          </table></div>
          <Pagination page={page} totalPages={pageCount(data)} onChange={setPage} />
        </>
      )}
    </>
  )
}
