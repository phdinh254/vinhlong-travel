import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg, money, imgUrl, TRANG_THAI_TOUR } from '../../api'
import { Badge, Pagination, Loading, Alert } from '../../components/ui.jsx'

export default function Tours() {
  const [q, setQ] = useState('')
  const [trangThai, setTrangThai] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [err, setErr] = useState('')

  const load = useCallback(() => {
    api.get('/api/admin/tours', { params: { q, trangThai, page } }).then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  }, [q, trangThai, page])
  useEffect(load, [load])

  const del = async (t) => {
    if (!confirm(`Xóa tour ${t.maTour}?`)) return
    try { await api.delete(`/api/admin/tours/${t.tourId}`); load() } catch (e) { setErr(errMsg(e)) }
  }

  return (
    <>
      <div className="admin-head">
        <h1>Tour</h1>
        <Link className="btn" to="/admin/tours/moi">+ Thêm tour</Link>
      </div>
      <div className="row" style={{ marginBottom: 14 }}>
        <input style={{ maxWidth: 300 }} placeholder="Tìm tên / mã tour" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <select style={{ maxWidth: 200 }} value={trangThai} onChange={(e) => { setTrangThai(e.target.value); setPage(0) }}>
          <option value="">Mọi trạng thái</option>
          {Object.entries(TRANG_THAI_TOUR).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}
        </select>
      </div>
      <Alert>{err}</Alert>
      {!data ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr><th></th><th>Mã</th><th>Tên tour</th><th>Thời lượng</th><th>Khu vực</th><th className="right">Giá từ</th><th>Trạng thái</th><th></th></tr></thead>
          <tbody>{data.content.map((t) => (
            <tr key={t.tourId}>
              <td>{t.anhDaiDien && <img src={imgUrl(t.anhDaiDien)} alt="" style={{ width: 64, height: 42, objectFit: 'cover', borderRadius: 6 }} />}</td>
              <td>{t.maTour}</td><td><Link to={`/admin/tours/${t.tourId}`}>{t.tenTour}</Link>{t.noiBat && <span className="badge warn" style={{ marginLeft: 6 }}>Nổi bật</span>}</td>
              <td>{t.thoiLuong}</td><td className="small">{t.khuVucs.join(', ')}</td><td className="right">{money(t.giaTu)}</td>
              <td><Badge map={TRANG_THAI_TOUR} value={t.trangThai} /></td>
              <td className="row"><Link className="btn secondary sm" to={`/admin/tours/${t.tourId}`}>Sửa</Link><button className="btn link small" onClick={() => del(t)}>Xóa</button></td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
      {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
    </>
  )
}
