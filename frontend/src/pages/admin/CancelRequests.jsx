import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime, pageCount, TRANG_THAI_YC_HUY } from '../../api'
import { Alert, Badge, Loading, Pagination } from '../../components/ui.jsx'
import { ProcessModal } from './BookingDetail.jsx'

export default function CancelRequests() {
  const [data, setData] = useState(null)
  const [sel, setSel] = useState(null)
  const [chiCho, setChiCho] = useState(true)
  const [page, setPage] = useState(0)
  const [err, setErr] = useState('')
  const load = () => api.get('/api/v1/admin/cancellation-requests', { params: { status: chiCho ? 'PENDING' : undefined, page, size: 20 } })
    .then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  useEffect(() => { load() }, [chiCho, page]) // eslint-disable-line react-hooks/exhaustive-deps
  return (
    <>
      <div className="admin-head"><h1>Yêu cầu hủy</h1>
        <label className="check"><input type="checkbox" checked={chiCho} onChange={(e) => { setPage(0); setChiCho(e.target.checked) }} /> Chỉ hiện yêu cầu chờ duyệt</label></div>
      <Alert>{err}</Alert>
      {!data ? <Loading /> : data.items.length === 0 ? <p className="muted">Không có yêu cầu.</p> : (
        <>
          <div className="table-wrap"><table>
            <thead><tr><th>Đơn</th><th>Loại</th><th>Hành khách</th><th>Lý do</th><th className="right">Đã thu</th><th className="right">Phí phạt</th><th className="right">Dự kiến hoàn</th><th>Thời gian</th><th>Kết quả</th><th></th></tr></thead>
            <tbody>{data.items.map((y) => (
              <tr key={y.id}><td><Link to={`/admin/don-dat/${y.bookingId}`}>{y.bookingCode}</Link><div className="muted small">{y.tourName}</div></td>
                <td>{y.scope === 'FULL' ? 'Toàn bộ' : 'Từng phần'}</td><td className="small">{y.passengerNames.join(', ')}</td>
                <td className="small">{y.reason}</td><td className="right">{money(y.paidAmount)}</td>
                <td className="right">{money(y.penaltyAmount)} <span className="muted small">({y.penaltyPercent}%)</span></td><td className="right">{money(y.estimatedRefund)}</td>
                <td className="small">{fmtDateTime(y.requestedAt)}</td><td><Badge map={TRANG_THAI_YC_HUY} value={y.status} /></td>
                <td>{y.status === 'PENDING' && <button className="btn sm" onClick={() => setSel(y)}>Xử lý</button>}</td></tr>
            ))}</tbody>
          </table></div>
          <Pagination page={page} totalPages={pageCount(data)} onChange={setPage} />
        </>
      )}
      {sel && <ProcessModal y={sel} code={sel.bookingCode} onClose={() => setSel(null)} onDone={() => { setSel(null); load() }} />}
    </>
  )
}
