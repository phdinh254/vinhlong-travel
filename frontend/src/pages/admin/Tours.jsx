import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg, money, imgUrl, pageCount, TRANG_THAI_TOUR } from '../../api'
import { Badge, Pagination, Loading, Alert } from '../../components/ui.jsx'
import { thoiLuong } from '../../tourUtils.js'

export default function Tours() {
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [err, setErr] = useState('')

  const load = useCallback(() => {
    api.get('/api/v1/admin/tours', { params: { q: q || undefined, status: status || undefined, page, size: 20 } })
      .then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  }, [q, status, page])
  useEffect(load, [load])

  const del = async (t) => {
    if (!confirm(`Xóa tour ${t.code}?`)) return
    try { await api.delete(`/api/v1/admin/tours/${t.id}`); load() } catch (e) { setErr(errMsg(e)) }
  }

  return (
    <>
      <div className="admin-head">
        <h1>Tour</h1>
        <Link className="btn" to="/admin/tours/moi">+ Thêm tour</Link>
      </div>
      <div className="row" style={{ marginBottom: 14 }}>
        <input style={{ maxWidth: 300 }} placeholder="Tìm tên / mã tour" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <select style={{ maxWidth: 200 }} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
          <option value="">Mọi trạng thái</option>
          {Object.entries(TRANG_THAI_TOUR).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}
        </select>
      </div>
      <Alert>{err}</Alert>
      {!data ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr><th></th><th>Mã</th><th>Tên tour</th><th>Thời lượng</th><th>Khu vực</th><th className="right">Giá từ</th><th>Trạng thái</th><th></th></tr></thead>
          <tbody>{data.items.map((t) => (
            <tr key={t.id}>
              <td>{t.coverImageUrl && <img src={imgUrl(t.coverImageUrl)} alt="" style={{ width: 64, height: 42, objectFit: 'cover', borderRadius: 6 }} />}</td>
              <td>{t.code}</td><td><Link to={`/admin/tours/${t.id}`}>{t.name}</Link>{t.featured && <span className="badge warn" style={{ marginLeft: 6 }}>Nổi bật</span>}</td>
              <td>{thoiLuong(t)}</td><td className="small">{t.regions.map((r) => r.name).join(', ')}</td><td className="right">{money(t.fromPrice)}</td>
              <td><Badge map={TRANG_THAI_TOUR} value={t.status} /></td>
              <td className="row"><Link className="btn secondary sm" to={`/admin/tours/${t.id}`}>Sửa</Link><button className="btn link small" onClick={() => del(t)}>Xóa</button></td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
      {data && <Pagination page={page} totalPages={pageCount(data)} onChange={setPage} />}
    </>
  )
}
