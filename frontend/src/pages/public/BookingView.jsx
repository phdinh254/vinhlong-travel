import { useCallback, useEffect, useState } from 'react'
import { useParams, useSearchParams, useLocation, Link } from 'react-router-dom'
import api, { errMsg, money } from '../../api'
import { useAuth } from '../../auth.jsx'
import { Alert, Loading, Modal } from '../../components/ui.jsx'
import BookingInfo from '../../components/BookingInfo.jsx'

/** Xem đơn + thanh toán VNPay + gửi yêu cầu hủy (khách đăng nhập, hoặc vãng lai bằng mã đơn + SĐT) */
export default function BookingView() {
  const { maDon } = useParams()
  const [sp] = useSearchParams()
  const { user } = useAuth()
  // Khách vãng lai: nhớ SĐT trong phiên để quay lại từ trang kết quả VNPay vẫn xem được đơn
  const sdt = sp.get('sdt') || sessionStorage.getItem(`sdt_${maDon}`) || ''
  useEffect(() => { if (sdt) sessionStorage.setItem(`sdt_${maDon}`, sdt) }, [maDon, sdt])
  const loc = useLocation()
  const [d, setD] = useState(null)
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const [showHuy, setShowHuy] = useState(false)
  const [huy, setHuy] = useState({ scope: 'FULL', reason: '', passengerIds: [] })
  const [huyErr, setHuyErr] = useState('')

  const load = useCallback(() => {
    const req = sdt
      ? api.get('/api/v1/bookings/lookup', { params: { code: maDon, phone: sdt } })
      : user ? api.get(`/api/v1/me/bookings/${maDon}`) : null
    if (!req) { setErr('Vui lòng nhập số điện thoại người đặt để xem đơn.'); return }
    req.then((r) => setD(r.data)).catch((e) => setErr(errMsg(e)))
  }, [maDon, sdt, user])
  useEffect(load, [load])

  const pay = async (type) => {
    setBusy(true); setErr('')
    try {
      const { data } = await api.post('/api/v1/payments/vnpay', { bookingCode: d.code, phone: sdt || d.contact.phone, type })
      window.location.href = data.paymentUrl
    } catch (e) { setErr(errMsg(e)); setBusy(false) }
  }

  const guiHuy = async () => {
    setHuyErr('')
    try {
      const body = { scope: huy.scope, reason: huy.reason || null, phone: sdt || d.contact.phone, passengerIds: huy.scope === 'PARTIAL' ? huy.passengerIds : [] }
      const { data } = await api.post(`/api/v1/bookings/${d.code}/cancellation-requests`, body)
      setD(data); setShowHuy(false)
    } catch (e) { setHuyErr(errMsg(e)) }
  }

  if (err && !d) return (
    <div className="container section"><Alert>{err}</Alert><p><Link to="/tra-cuu">← Tra cứu đơn khác</Link></p></div>
  )
  if (!d) return <div className="container section"><Loading /></div>

  const coTheTT = ['PENDING_PAYMENT', 'DEPOSITED'].includes(d.status) && d.remainingAmount > 0
  const coTheHuy = ['PENDING_PAYMENT', 'DEPOSITED', 'PAID'].includes(d.status)
    && !d.cancellationRequests.some((y) => y.status === 'PENDING') && new Date(d.departure.departureAt) > new Date()
  const hkHieuLuc = d.passengers.filter((h) => h.status === 'ACTIVE')

  return (
    <div className="container section grid grid-main">
      <div>
        {loc.state?.moi && <div className="alert ok" style={{ marginBottom: 12 }}>Đặt tour thành công! Hãy lưu lại mã đơn <b>{d.code}</b> và số điện thoại để tra cứu.</div>}
        <BookingInfo d={d} />
      </div>
      <aside className="stack">
        <div className="card" style={{ position: 'sticky', top: 80 }}>
          <h3>Thanh toán</h3>
          <Alert>{err}</Alert>
          {coTheTT ? (
            <>
              <p className="small">Còn phải thanh toán: <b className="price">{money(d.remainingAmount)}</b></p>
              {d.paidAmount === 0 && d.minimumFirstPayment < d.totalAmount && (
                <button className="btn secondary" style={{ width: '100%', marginBottom: 8 }} disabled={busy} onClick={() => pay('DEPOSIT')}>
                  Đặt cọc {money(d.minimumFirstPayment)} qua VNPay
                </button>
              )}
              <button className="btn" style={{ width: '100%' }} disabled={busy} onClick={() => pay('FULL')}>
                Thanh toán {money(d.remainingAmount)} qua VNPay
              </button>
              <p className="muted small">Bạn sẽ được chuyển sang cổng VNPay để thanh toán. Link thanh toán có hiệu lực 15 phút.</p>
            </>
          ) : <p className="muted small">Đơn không cần thanh toán thêm.</p>}
          {coTheHuy && (
            <>
              <hr style={{ border: 0, borderTop: '1px solid var(--line)', margin: '16px 0' }} />
              <button className="btn danger sm" onClick={() => setShowHuy(true)}>Gửi yêu cầu hủy</button>
            </>
          )}
        </div>
      </aside>

      {showHuy && (
        <Modal title="Yêu cầu hủy tour" onClose={() => setShowHuy(false)}>
          <div className="field">
            <label className="check"><input type="radio" checked={huy.scope === 'FULL'} onChange={() => setHuy({ ...huy, scope: 'FULL' })} /> Hủy toàn bộ đơn</label>
            {hkHieuLuc.length > 1 && <label className="check"><input type="radio" checked={huy.scope === 'PARTIAL'} onChange={() => setHuy({ ...huy, scope: 'PARTIAL' })} /> Hủy một số hành khách</label>}
          </div>
          {huy.scope === 'PARTIAL' && (
            <div className="box field">
              {hkHieuLuc.map((h) => (
                <label key={h.id} className="check">
                  <input type="checkbox" checked={huy.passengerIds.includes(h.id)}
                    onChange={(e) => setHuy({ ...huy, passengerIds: e.target.checked ? [...huy.passengerIds, h.id] : huy.passengerIds.filter((x) => x !== h.id) })} />
                  {h.fullName} ({h.passengerTypeName})
                </label>
              ))}
            </div>
          )}
          <div className="field"><label>Lý do</label><textarea value={huy.reason} onChange={(e) => setHuy({ ...huy, reason: e.target.value })} /></div>
          <p className="muted small">Phí phạt được tính theo chính sách hủy đã áp dụng khi bạn đặt tour. Đơn chưa thanh toán sẽ được hủy ngay; đơn đã thanh toán chờ nhân viên duyệt.</p>
          <Alert>{huyErr}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button className="btn secondary" onClick={() => setShowHuy(false)}>Đóng</button>
            <button className="btn danger" onClick={guiHuy}>Gửi yêu cầu</button>
          </div>
        </Modal>
      )}
    </div>
  )
}
