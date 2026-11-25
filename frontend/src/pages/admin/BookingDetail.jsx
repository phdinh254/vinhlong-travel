import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime } from '../../api'
import { Alert, Loading, Modal, ImagePicker } from '../../components/ui.jsx'
import BookingInfo from '../../components/BookingInfo.jsx'

/** Admin xử lý đơn: ghi nhận thanh toán thủ công, duyệt hủy, hoàn tiền, chuyển đợt, đổi trạng thái */
export default function BookingDetail() {
  const { id } = useParams()
  const [d, setD] = useState(null)
  const [err, setErr] = useState('')
  const [modal, setModal] = useState(null)

  const load = () => api.get(`/api/v1/admin/bookings/${id}`).then((r) => setD(r.data)).catch((e) => setErr(errMsg(e)))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { load() }, [id])

  if (!d) return err ? <Alert>{err}</Alert> : <Loading />
  const active = ['PENDING_PAYMENT', 'DEPOSITED', 'PAID'].includes(d.status)
  const pending = d.cancellationRequests.find((y) => y.status === 'PENDING')
  const done = (data) => { setD(data); setModal(null) }

  return (
    <>
      <div className="admin-head">
        <h1>Đơn {d.code}</h1>
        <div className="row">
          <Link className="btn secondary" to="/admin/don-dat">← Danh sách</Link>
          {(active || d.status === 'EXPIRED') && d.remainingAmount > 0 && <button className="btn" onClick={() => setModal('pay')}>Ghi nhận thanh toán</button>}
          {d.refundDue > 0 && <button className="btn secondary" onClick={() => setModal('refund')}>Hoàn tiền</button>}
          {active && <button className="btn secondary" onClick={() => setModal('transfer')}>Chuyển đợt</button>}
          {d.status === 'PAID' && <button className="btn secondary" onClick={() => setModal('done')}>Hoàn thành</button>}
          {d.status === 'PENDING_PAYMENT' && d.paidAmount === 0 && <button className="btn danger" onClick={() => setModal('cancel')}>Hủy đơn</button>}
        </div>
      </div>
      <Alert>{err}</Alert>
      {pending && (
        <div className="alert" style={{ marginBottom: 14 }}>
          Có yêu cầu {pending.scope === 'FULL' ? 'hủy toàn bộ' : `hủy ${pending.passengerNames.length} hành khách`} chờ xử lý (phí phạt {money(pending.penaltyAmount)}, dự kiến hoàn {money(pending.estimatedRefund)}).{' '}
          <button className="btn sm" onClick={() => setModal({ type: 'process', y: pending })}>Xử lý</button>
        </div>
      )}
      {d.status === 'CANCELLED' && d.refundDue > 0 && (
        <div className="alert" style={{ marginBottom: 14 }}>Đơn đã hủy nhưng còn {money(d.refundDue)} cần hoàn cho khách.</div>
      )}
      <BookingInfo d={d} admin />

      {modal === 'pay' && <PayModal d={d} onClose={() => setModal(null)} onDone={done} />}
      {modal === 'refund' && <RefundModal d={d} onClose={() => setModal(null)} onDone={done} />}
      {modal === 'transfer' && <TransferModal d={d} onClose={() => setModal(null)} onDone={done} />}
      {(modal === 'done' || modal === 'cancel') && <StatusModal d={d} status={modal === 'done' ? 'COMPLETED' : 'CANCELLED'} onClose={() => setModal(null)} onDone={done} />}
      {modal?.type === 'process' && <ProcessModal y={modal.y} code={d.code} onClose={() => setModal(null)} onDone={done} />}
    </>
  )
}

function useSubmit(onDone) {
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const run = async (fn) => { setBusy(true); setErr(''); try { onDone((await fn()).data) } catch (e) { setErr(errMsg(e)) } finally { setBusy(false) } }
  return { err, busy, run }
}

function PayModal({ d, onClose, onDone }) {
  const [f, setF] = useState({ amount: d.remainingAmount, method: 'BANK_TRANSFER', transactionNo: '', proofUrl: '', note: '' })
  const s = useSubmit(onDone)
  const body = () => ({ amount: Number(f.amount), method: f.method, transactionNo: f.transactionNo || null, proofUrl: f.proofUrl || null, note: f.note || null })
  return (
    <Modal title="Ghi nhận thanh toán thủ công" onClose={onClose}>
      <p className="muted small">Còn phải thu {money(d.remainingAmount)}{d.paidAmount === 0 && ` · lần đầu tối thiểu ${money(d.minimumFirstPayment)}`}.</p>
      <div className="grid grid-2">
        <div className="field"><label>Số tiền</label><input type="number" value={f.amount} onChange={(e) => setF({ ...f, amount: e.target.value })} /></div>
        <div className="field"><label>Phương thức</label><select value={f.method} onChange={(e) => setF({ ...f, method: e.target.value })}><option value="BANK_TRANSFER">Chuyển khoản</option><option value="CASH">Tiền mặt</option></select></div>
      </div>
      <div className="field"><label>Mã giao dịch ngân hàng</label><input value={f.transactionNo} onChange={(e) => setF({ ...f, transactionNo: e.target.value })} /></div>
      <ImagePicker label="Ảnh chứng từ / biên lai" value={f.proofUrl} onChange={(v) => setF({ ...f, proofUrl: v })} />
      <div className="field"><label>Ghi chú</label><input value={f.note} onChange={(e) => setF({ ...f, note: e.target.value })} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" disabled={s.busy} onClick={() => s.run(() => api.post(`/api/v1/admin/bookings/${d.id}/payments`, body()))}>Xác nhận</button></div>
    </Modal>
  )
}

function RefundModal({ d, onClose, onDone }) {
  const approved = d.cancellationRequests.filter((y) => y.status === 'APPROVED')
  const [f, setF] = useState({ cancellationRequestId: approved[0]?.id || '', amount: d.refundDue, method: 'BANK_TRANSFER', transactionNo: '', proofUrl: '', note: '' })
  const s = useSubmit(onDone)
  const body = () => ({
    amount: Number(f.amount), method: f.method, transactionNo: f.transactionNo || null, proofUrl: f.proofUrl || null, note: f.note || null,
    cancellationRequestId: f.cancellationRequestId ? Number(f.cancellationRequestId) : null,
  })
  return (
    <Modal title="Ghi nhận hoàn tiền" onClose={onClose}>
      <p className="muted small">Cần hoàn tối đa {money(d.refundDue)}. Có thể hoàn nhiều lần.</p>
      <div className="grid grid-3">
        <div className="field"><label>Theo yêu cầu hủy</label><select value={f.cancellationRequestId} onChange={(e) => setF({ ...f, cancellationRequestId: e.target.value })}><option value="">— Không —</option>{approved.map((y) => <option key={y.id} value={y.id}>#{y.id} – dự kiến {money(y.estimatedRefund)}</option>)}</select></div>
        <div className="field"><label>Số tiền hoàn</label><input type="number" value={f.amount} onChange={(e) => setF({ ...f, amount: e.target.value })} /></div>
        <div className="field"><label>Phương thức</label><select value={f.method} onChange={(e) => setF({ ...f, method: e.target.value })}><option value="BANK_TRANSFER">Chuyển khoản</option><option value="CASH">Tiền mặt</option></select></div>
      </div>
      <div className="field"><label>Mã giao dịch hoàn</label><input value={f.transactionNo} onChange={(e) => setF({ ...f, transactionNo: e.target.value })} /></div>
      <ImagePicker label="Ảnh chứng từ hoàn tiền" value={f.proofUrl} onChange={(v) => setF({ ...f, proofUrl: v })} />
      <div className="field"><label>Ghi chú</label><input value={f.note} onChange={(e) => setF({ ...f, note: e.target.value })} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" disabled={s.busy} onClick={() => s.run(() => api.post(`/api/v1/admin/bookings/${d.id}/refunds`, body()))}>Ghi nhận</button></div>
    </Modal>
  )
}

function TransferModal({ d, onClose, onDone }) {
  const [dots, setDots] = useState([])
  const [f, setF] = useState({ departureId: '', reason: '' })
  const s = useSubmit(onDone)
  useEffect(() => {
    api.get(`/api/v1/admin/tours/${d.tour.id}/departures`)
      .then((r) => setDots(r.data.filter((x) => x.id !== d.departure.id && x.status === 'OPEN'))).catch(() => setDots([]))
  }, [d])
  return (
    <Modal title="Chuyển đợt khởi hành" onClose={onClose}>
      <p className="muted small">Giá vé được tính lại theo tuổi và bảng giá của đợt mới; chênh lệch được ghi vào lịch sử chuyển đợt.</p>
      <div className="field"><label>Đợt mới</label><select value={f.departureId} onChange={(e) => setF({ ...f, departureId: e.target.value })}><option value="">— Chọn —</option>{dots.map((x) => <option key={x.id} value={x.id}>{x.code} · {fmtDateTime(x.departureAt)} · còn {x.seatsLeft} chỗ</option>)}</select></div>
      <div className="field"><label>Lý do</label><input value={f.reason} onChange={(e) => setF({ ...f, reason: e.target.value })} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" disabled={s.busy || !f.departureId} onClick={() => s.run(() => api.post(`/api/v1/admin/bookings/${d.id}/transfer`, { departureId: Number(f.departureId), reason: f.reason || null }))}>Chuyển đợt</button></div>
    </Modal>
  )
}

function StatusModal({ d, status, onClose, onDone }) {
  const [reason, setReason] = useState('')
  const s = useSubmit(onDone)
  return (
    <Modal title={status === 'CANCELLED' ? 'Hủy đơn (trả chỗ)' : 'Đánh dấu hoàn thành'} onClose={onClose}>
      {status === 'CANCELLED' && <p className="alert small">Chỉ hủy trực tiếp được đơn chưa thu tiền; chỗ được trả về đợt. Đơn đã thu tiền thì xử lý qua yêu cầu hủy.</p>}
      <div className="field"><label>Lý do</label><input value={reason} onChange={(e) => setReason(e.target.value)} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}><button className={`btn ${status === 'CANCELLED' ? 'danger' : ''}`} disabled={s.busy} onClick={() => s.run(() => api.patch(`/api/v1/admin/bookings/${d.id}/status`, { status, reason: reason || null }))}>Xác nhận</button></div>
    </Modal>
  )
}

/** Duyệt / từ chối yêu cầu hủy. y: yêu cầu hủy (từ chi tiết đơn hoặc danh sách yêu cầu); code: mã đơn để hiển thị */
export function ProcessModal({ y, code, onClose, onDone }) {
  const [note, setNote] = useState('')
  const s = useSubmit(onDone)
  return (
    <Modal title={`Xử lý yêu cầu hủy – đơn ${code}`} onClose={onClose}>
      <p><b>{y.scope === 'FULL' ? 'Hủy toàn bộ' : 'Hủy từng phần'}</b> · {y.passengerNames.join(', ')}</p>
      <p className="small">Lý do: {y.reason || '—'}</p>
      <p className="small">Phí phạt theo chính sách ({y.penaltyPercent}%): <b>{money(y.penaltyAmount)}</b> · Dự kiến hoàn: <b>{money(y.estimatedRefund)}</b></p>
      <div className="field"><label>Phản hồi cho khách</label><textarea value={note} onChange={(e) => setNote(e.target.value)} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}>
        <button className="btn secondary" disabled={s.busy} onClick={() => s.run(() => api.post(`/api/v1/admin/cancellation-requests/${y.id}/reject`, { note: note || null }))}>Từ chối</button>
        <button className="btn danger" disabled={s.busy} onClick={() => s.run(() => api.post(`/api/v1/admin/cancellation-requests/${y.id}/approve`, { note: note || null }))}>Chấp nhận hủy</button>
      </div>
    </Modal>
  )
}
