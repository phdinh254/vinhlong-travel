import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime, TRANG_THAI_DON } from '../../api'
import { Badge, Pagination, Loading, Alert } from '../../components/ui.jsx'

export default function Bookings() {
  const [sp, setSp] = useSearchParams()
  const [data, setData] = useState(null)
  const [err, setErr] = useState('')
  const [q, setQ] = useState(sp.get('q') || '')
  const page = Number(sp.get('page') || 0)

  useEffect(() => {
    setData(null)
    api.get('/api/admin/bookings', { params: Object.fromEntries(sp.entries()) }).then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  }, [sp])

  const set = (k, v) => { const n = Object.fromEntries(sp.entries()); if (v) n[k] = v; else delete n[k]; if (k !== 'page') delete n.page; setSp(n) }

  return (
    <>
      <div className="admin-head"><h1>Đơn đặt tour</h1></div>
      <div className="row" style={{ marginBottom: 14 }}>
        <form onSubmit={(e) => { e.preventDefault(); set('q', q) }}><input style={{ width: 300 }} placeholder="Mã đơn / tên / SĐT người đặt" value={q} onChange={(e) => setQ(e.target.value)} /></form>
        <select style={{ maxWidth: 220 }} value={sp.get('trangThai') || ''} onChange={(e) => set('trangThai', e.target.value)}>
          <option value="">Mọi trạng thái</option>
          {Object.entries(TRANG_THAI_DON).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}
        </select>
        {sp.get('dotId') && <span className="badge info">Lọc theo đợt #{sp.get('dotId')} <button className="btn link small" onClick={() => set('dotId', '')}>✕</button></span>}
      </div>
      <Alert>{err}</Alert>
      {!data ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Mã đơn</th><th>Tour / Đợt</th><th>Người đặt</th><th>Khách</th><th className="right">Tổng</th><th className="right">Đã thu</th><th>Trạng thái</th><th>Ngày đặt</th></tr></thead>
          <tbody>{data.content.map((x) => (
            <tr key={x.donDatTourId}>
              <td><Link to={`/admin/don-dat/${x.donDatTourId}`}>{x.maDon}</Link></td>
              <td>{x.tenTour}<div className="muted small">{x.maDot} · {fmtDateTime(x.ngayGioDi)}</div></td>
              <td>{x.tenNguoiDat}<div className="muted small">{x.sdtNguoiDat}</div></td>
              <td>{x.soHanhKhach}</td><td className="right">{money(x.tongTien)}</td><td className="right">{money(x.daThanhToan)}</td>
              <td><Badge map={TRANG_THAI_DON} value={x.trangThai} /></td><td className="small">{fmtDateTime(x.ngayDat)}</td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
      {data && <Pagination page={page} totalPages={data.totalPages} onChange={(p) => set('page', String(p))} />}
    </>
  )
}
