import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime, TRANG_THAI_DON } from '../../api'
import { Alert, Badge, Loading } from '../../components/ui.jsx'

function BookingTable({ rows, extra }) {
  return (
    <div className="table-wrap"><table>
      <thead><tr><th>Mã đơn</th><th>Tour</th><th>Người đặt</th><th>Khởi hành</th><th className="right">Tổng</th>{extra && <th className="right">{extra[0]}</th>}<th>Trạng thái</th><th>Ngày đặt</th></tr></thead>
      <tbody>{rows.map((x) => (
        <tr key={x.id}><td><Link to={`/admin/don-dat/${x.id}`}>{x.code}</Link></td><td>{x.tourName}</td><td>{x.contactName}<div className="muted small">{x.contactPhone}</div></td>
          <td>{fmtDateTime(x.departureAt)}</td><td className="right">{money(x.totalAmount)}</td>{extra && <td className="right">{extra[1](x)}</td>}
          <td><Badge map={TRANG_THAI_DON} value={x.status} /></td><td>{fmtDateTime(x.createdAt)}</td></tr>
      ))}</tbody>
    </table></div>
  )
}

export default function Dashboard() {
  const [d, setD] = useState(null)
  const [range, setRange] = useState({ from: '', to: '' })
  const [err, setErr] = useState('')
  const load = () => api.get('/api/v1/admin/dashboard', { params: { from: range.from || undefined, to: range.to || undefined } })
    .then((r) => { setD(r.data); setRange({ from: r.data.from, to: r.data.to }) }).catch((e) => setErr(errMsg(e)))
  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps
  if (!d) return err ? <Alert>{err}</Alert> : <Loading />
  const by = d.bookingsByStatus || {}
  const stats = [
    ['Tour đang bán', d.toursOnSale, '/admin/tours'], ['Đơn mới trong kỳ', d.newBookings, '/admin/don-dat'],
    ['Chờ thanh toán', by.PENDING_PAYMENT ?? 0, '/admin/don-dat?status=PENDING_PAYMENT'], ['Đã cọc', by.DEPOSITED ?? 0, '/admin/don-dat?status=DEPOSITED'],
    ['Đã thanh toán', by.PAID ?? 0, '/admin/don-dat?status=PAID'], ['Đơn cần hoàn tiền', d.bookingsNeedingRefund, '/admin/don-dat?needsRefund=true'],
    ['Yêu cầu hủy chờ xử lý', d.pendingCancellationRequests, '/admin/yeu-cau-huy'], ['Liên hệ mới', d.newContactMessages, '/admin/lien-he'],
    ['Tiền đã thu', money(d.collected), null], ['Tiền đã hoàn', money(d.refunded), null], ['Doanh thu ròng', money(d.netRevenue), null],
  ]
  return (
    <>
      <div className="admin-head"><h1>Tổng quan</h1>
        <form className="row" onSubmit={(e) => { e.preventDefault(); load() }}>
          <input type="date" value={range.from || ''} onChange={(e) => setRange({ ...range, from: e.target.value })} />
          <span className="muted">→</span>
          <input type="date" value={range.to || ''} onChange={(e) => setRange({ ...range, to: e.target.value })} />
          <button className="btn secondary sm">Xem</button>
        </form>
      </div>
      <Alert>{err}</Alert>
      <div className="grid grid-4">
        {stats.map(([l, v, to]) => (
          <div key={l} className="card stat">
            <div className="muted small">{l}</div>
            <div className="num">{to ? <Link to={to} style={{ color: 'inherit' }}>{v}</Link> : v}</div>
          </div>
        ))}
      </div>
      {d.bookingsNeedingRefundList.length > 0 && (
        <>
          <h3 style={{ marginTop: 28 }}>Đơn cần hoàn tiền</h3>
          <BookingTable rows={d.bookingsNeedingRefundList} extra={['Cần hoàn', (x) => money(x.refundDue)]} />
        </>
      )}
      <h3 style={{ marginTop: 28 }}>Đơn mới nhất</h3>
      <BookingTable rows={d.recentBookings} />
    </>
  )
}
