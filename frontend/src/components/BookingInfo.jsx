import { money, fmtDate, fmtDateTime, imgUrl, hhmm, TRANG_THAI_DON, TRANG_THAI_TT, TRANG_THAI_YC_HUY, PHUONG_THUC_TT } from '../api'
import { Badge } from './ui.jsx'

/** Hiển thị chi tiết đơn (BookingDetail của API) – dùng chung cho khách và admin */
export default function BookingInfo({ d, admin = false }) {
  return (
    <div className="stack">
      <div className="card">
        <div className="row between">
          <h2 style={{ margin: 0 }}>Đơn {d.code}</h2>
          <Badge map={TRANG_THAI_DON} value={d.status} />
        </div>
        <p className="muted">{d.tour.name} · Đợt {d.departure.code} · Khởi hành {fmtDateTime(d.departure.departureAt)}</p>
        <div className="grid grid-3 small">
          <div><b>Người đặt:</b> {d.contact.name}<br /><b>SĐT:</b> {d.contact.phone}<br /><b>Email:</b> {d.contact.email || '—'}</div>
          <div><b>Ngày đặt:</b> {fmtDateTime(d.createdAt)}<br />{d.status === 'PENDING_PAYMENT' && d.holdUntil && <><b>Giữ chỗ đến:</b> {fmtDateTime(d.holdUntil)}<br /></>}<b>Số chỗ:</b> {d.seats}</div>
          <div><b>Ghi chú:</b> {d.note || '—'}{d.cancellationPolicy && <><br /><b>Chính sách hủy:</b> {d.cancellationPolicy.name}</>}</div>
        </div>
      </div>

      <div className="card">
        <h3>Hành khách</h3>
        <div className="table-wrap"><table>
          <thead><tr><th>Họ tên</th><th>Ngày sinh</th><th>Loại khách</th><th>Điểm đón</th><th className="right">Giá vé</th><th className="right">Phí đón</th><th>Trạng thái</th></tr></thead>
          <tbody>{d.passengers.map((h) => (
            <tr key={h.id} style={h.status === 'CANCELLED' ? { opacity: .5, textDecoration: 'line-through' } : undefined}>
              <td>{h.fullName}{admin && h.idNumber && <div className="muted small">{h.idNumber}</div>}</td>
              <td>{fmtDate(h.birthDate)} <span className="muted small">({h.ageAtDeparture} tuổi)</span></td><td>{h.passengerTypeName}</td>
              <td>{h.pickupPointName ? `${h.pickupPointName} · ${hhmm(h.pickupTime)}` : '—'}</td>
              <td className="right">{money(h.price)}</td><td className="right">{money(h.pickupFee)}</td>
              <td>{h.status === 'CANCELLED' ? 'Đã hủy' : 'Hiệu lực'}</td>
            </tr>
          ))}</tbody>
        </table></div>
        {d.surcharges.length > 0 && (
          <div style={{ marginTop: 10 }}>{d.surcharges.map((p) => <div key={p.surchargeId} className="summary-line small"><span>{p.name} × {p.quantity}</span><span>{money(p.amount)}</span></div>)}</div>
        )}
        <div style={{ maxWidth: 360, marginLeft: 'auto', marginTop: 10 }}>
          <div className="summary-line"><span>Tiền vé</span><span>{money(d.ticketTotal)}</span></div>
          {d.pickupTotal > 0 && <div className="summary-line"><span>Phí đón</span><span>{money(d.pickupTotal)}</span></div>}
          <div className="summary-line"><span>Phụ thu</span><span>{money(d.surchargeTotal)}</span></div>
          {d.cancellationFee > 0 && <div className="summary-line"><span>Phí hủy</span><span>{money(d.cancellationFee)}</span></div>}
          <div className="summary-line total"><span>Tổng cộng</span><span>{money(d.totalAmount)}</span></div>
          <div className="summary-line"><span>Đã thanh toán</span><span style={{ color: 'var(--green)' }}>{money(d.paidAmount)}</span></div>
          <div className="summary-line"><b>Còn lại</b><b className="price">{money(d.remainingAmount)}</b></div>
          {d.refundDue > 0 && <div className="summary-line"><b>Cần hoàn cho khách</b><b style={{ color: 'var(--red, #c0392b)' }}>{money(d.refundDue)}</b></div>}
        </div>
      </div>

      {d.payments.length > 0 && (
        <div className="card">
          <h3>Thanh toán</h3>
          <div className="table-wrap"><table>
            <thead><tr><th>Thời gian</th><th>Phương thức</th><th>Mã GD</th><th className="right">Số tiền</th><th>Trạng thái</th>{admin && <th>Xác nhận</th>}</tr></thead>
            <tbody>{d.payments.map((t) => (
              <tr key={t.id}><td>{fmtDateTime(t.paidAt || t.createdAt)}</td><td>{PHUONG_THUC_TT[t.method] || t.method}</td>
                <td>{t.transactionNo || t.reference || '—'}{t.proofUrl && <> · <a href={imgUrl(t.proofUrl)} target="_blank" rel="noreferrer">chứng từ</a></>}</td>
                <td className="right">{money(t.amount)}</td><td><Badge map={TRANG_THAI_TT} value={t.status} /></td>{admin && <td>{t.confirmedBy || '—'}</td>}</tr>
            ))}</tbody>
          </table></div>
        </div>
      )}

      {d.cancellationRequests.length > 0 && (
        <div className="card">
          <h3>Yêu cầu hủy</h3>
          {d.cancellationRequests.map((y) => (
            <div key={y.id} className="box" style={{ marginBottom: 8 }}>
              <div className="row between"><b>{y.scope === 'FULL' ? 'Hủy toàn bộ' : 'Hủy từng phần'} · {fmtDateTime(y.requestedAt)}</b><Badge map={TRANG_THAI_YC_HUY} value={y.status} /></div>
              <div className="small">Hành khách: {y.passengerNames.join(', ')}</div>
              <div className="small">Lý do: {y.reason || '—'}</div>
              <div className="small">Phí phạt ({y.penaltyPercent}%): <b>{money(y.penaltyAmount)}</b> · Dự kiến hoàn: <b>{money(y.estimatedRefund)}</b></div>
              {y.decisionNote && <div className="small muted">Phản hồi: {y.decisionNote}</div>}
            </div>
          ))}
        </div>
      )}

      {d.moneyHistory.length > 0 && (
        <div className="card">
          <h3>Sổ tiền</h3>
          {d.moneyHistory.map((m, i) => (
            <div key={i} className="summary-line small">
              <span>{fmtDateTime(m.at)} · {m.direction === 'IN' ? 'Thu' : 'Hoàn'} · {PHUONG_THUC_TT[m.method] || m.method}{m.transactionNo && ` · ${m.transactionNo}`}{admin && m.handledBy && ` · ${m.handledBy}`}</span>
              <b style={{ color: m.direction === 'IN' ? 'var(--green)' : undefined }}>{m.direction === 'IN' ? '+' : '−'}{money(Math.abs(m.amount))}</b>
            </div>
          ))}
        </div>
      )}

      {d.transfers.length > 0 && (
        <div className="card">
          <h3>Lịch sử chuyển đợt</h3>
          {d.transfers.map((c, i) => <div key={i} className="small">{fmtDateTime(c.transferredAt)}: {c.fromDepartureCode} → {c.toDepartureCode} (chênh lệch {money(c.priceDifference)}) {c.reason && `– ${c.reason}`}</div>)}
        </div>
      )}

      <div className="card">
        <h3>Lịch sử trạng thái</h3>
        {d.history.map((l, i) => (
          <div key={i} className="small" style={{ padding: '4px 0', borderBottom: '1px dashed var(--line)' }}>
            {fmtDateTime(l.changedAt)} · {l.fromStatus ? `${TRANG_THAI_DON[l.fromStatus]?.[0] || l.fromStatus} → ` : ''}<b>{TRANG_THAI_DON[l.toStatus]?.[0] || l.toStatus}</b>{l.changedBy && ` · ${l.changedBy}`}{l.reason && <span className="muted"> – {l.reason}</span>}
          </div>
        ))}
      </div>
    </div>
  )
}
