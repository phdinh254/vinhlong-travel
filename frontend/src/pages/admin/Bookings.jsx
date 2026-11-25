import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime, pageCount, TRANG_THAI_DON } from '../../api'
import { Badge, Pagination, Loading, Alert } from '../../components/ui.jsx'

/** Danh sách đơn; tham số lọc trên URL trùng tên với API: q, status, tourId, departureId, needsRefund, page */
export default function Bookings() {
  const [sp, setSp] = useSearchParams()
  const [data, setData] = useState(null)
  const [err, setErr] = useState('')
  const [q, setQ] = useState(sp.get('q') || '')
  const page = Number(sp.get('page') || 0)

  useEffect(() => {
    setData(null)
    api.get('/api/v1/admin/bookings', { params: { ...Object.fromEntries(sp.entries()), size: 20 } }).then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  }, [sp])

  const set = (k, v) => { const n = Object.fromEntries(sp.entries()); if (v) n[k] = v; else delete n[k]; if (k !== 'page') delete n.page; setSp(n) }

  return (
    <>
      <div className="admin-head"><h1>Đơn đặt tour</h1></div>
      <div className="row" style={{ marginBottom: 14 }}>
        <form onSubmit={(e) => { e.preventDefault(); set('q', q) }}><input style={{ width: 300 }} placeholder="Mã đơn / tên / SĐT người đặt" value={q} onChange={(e) => setQ(e.target.value)} /></form>
        <select style={{ maxWidth: 220 }} value={sp.get('status') || ''} onChange={(e) => set('status', e.target.value)}>
          <option value="">Mọi trạng thái</option>
          {Object.entries(TRANG_THAI_DON).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}
        </select>
        <label className="check"><input type="checkbox" checked={sp.get('needsRefund') === 'true'} onChange={(e) => set('needsRefund', e.target.checked ? 'true' : '')} /> Cần hoàn tiền</label>
        {sp.get('departureId') && <span className="badge info">Lọc theo đợt #{sp.get('departureId')} <button className="btn link small" onClick={() => set('departureId', '')}>✕</button></span>}
      </div>
      <Alert>{err}</Alert>
      {!data ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Mã đơn</th><th>Tour / Đợt</th><th>Người đặt</th><th>Khách</th><th className="right">Tổng</th><th className="right">Đã thu</th><th>Trạng thái</th><th>Ngày đặt</th></tr></thead>
          <tbody>{data.items.map((x) => (
            <tr key={x.id}>
              <td><Link to={`/admin/don-dat/${x.id}`}>{x.code}</Link></td>
              <td>{x.tourName}<div className="muted small">{x.departureCode} · {fmtDateTime(x.departureAt)}</div></td>
              <td>{x.contactName}<div className="muted small">{x.contactPhone}</div></td>
              <td>{x.passengerCount}</td><td className="right">{money(x.totalAmount)}</td>
              <td className="right">{money(x.paidAmount)}{x.refundDue > 0 && <div className="small" style={{ color: '#c0392b' }}>Cần hoàn {money(x.refundDue)}</div>}</td>
              <td><Badge map={TRANG_THAI_DON} value={x.status} /></td><td className="small">{fmtDateTime(x.createdAt)}</td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
      {data && <Pagination page={page} totalPages={pageCount(data)} onChange={(p) => set('page', String(p))} />}
    </>
  )
}
