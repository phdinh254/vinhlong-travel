import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime, hhmm, CACH_TINH_PHU_THU } from '../../api'
import { useAuth } from '../../auth.jsx'
import { Alert, Loading } from '../../components/ui.jsx'

const emptyPax = () => ({ fullName: '', birthDate: '', gender: 'MALE', phone: '', idNumber: '', pickupPointId: '' })
const orNull = (v) => (v === '' || v == null ? null : v)

/** Đặt tour: khách đăng nhập hoặc khách vãng lai */
export default function Booking() {
  const { dotId } = useParams()
  const { user } = useAuth()
  const nav = useNavigate()
  const [dot, setDot] = useState(null)
  const [tour, setTour] = useState(null)
  const [form, setForm] = useState({ contactName: user?.fullName || '', contactPhone: user?.phone || '', contactEmail: user?.email || '', note: '' })
  const [pax, setPax] = useState([{ ...emptyPax(), fullName: user?.fullName || '' }])
  const [phuThu, setPhuThu] = useState({}) // surchargeId -> quantity
  const [quote, setQuote] = useState(null)
  const [quoteErr, setQuoteErr] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.get(`/api/v1/departures/${dotId}`).then(async (r) => {
      setDot(r.data)
      setTour((await api.get(`/api/v1/tours/${r.data.tourId}`)).data)
    }).catch((e) => setErr(errMsg(e)))
  }, [dotId])

  const passengers = () => pax.map((p) => ({
    fullName: p.fullName, birthDate: p.birthDate, gender: orNull(p.gender), phone: orNull(p.phone),
    idNumber: orNull(p.idNumber), pickupPointId: p.pickupPointId ? Number(p.pickupPointId) : null,
  }))
  const surcharges = () => Object.entries(phuThu).map(([id, sl]) => ({ surchargeId: Number(id), quantity: Number(sl) || 1 }))

  // Báo giá tự động khi mọi hành khách đã có họ tên và ngày sinh
  useEffect(() => {
    if (!dot || pax.some((p) => !p.birthDate || !p.fullName)) { setQuote(null); return }
    const h = setTimeout(() => {
      api.post('/api/v1/bookings/quote', { departureId: Number(dotId), passengers: passengers(), surcharges: surcharges() })
        .then((r) => { setQuote(r.data); setQuoteErr('') })
        .catch((e) => { setQuote(null); setQuoteErr(errMsg(e)) })
    }, 400)
    return () => clearTimeout(h)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pax, phuThu, dot])

  const setP = (i, k, v) => setPax(pax.map((p, j) => (j === i ? { ...p, [k]: v } : p)))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try {
      const { data } = await api.post('/api/v1/bookings', {
        departureId: Number(dotId), ...form, contactEmail: orNull(form.contactEmail), note: orNull(form.note),
        passengers: passengers(), surcharges: surcharges(),
      })
      sessionStorage.setItem(`sdt_${data.code}`, data.contact.phone)
      nav(`/don/${data.code}?sdt=${encodeURIComponent(data.contact.phone)}`, { state: { moi: true } })
    } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }

  if (err && !dot) return <div className="container section"><Alert>{err}</Alert></div>
  if (!dot || !tour) return <div className="container section"><Loading /></div>

  return (
    <div className="container section">
      <p><Link to={`/tour/${tour.id}`}>← {tour.name}</Link></p>
      <h1>Đặt tour</h1>
      <p className="muted">Khởi hành {fmtDateTime(dot.departureAt)} · Còn {dot.seatsLeft} chỗ · Mã đợt {dot.code} · Hạn đặt {fmtDateTime(dot.bookingDeadline)}</p>
      {!dot.bookable && <Alert>Đợt khởi hành này hiện không nhận đặt chỗ.</Alert>}
      {!user && <div className="alert" style={{ marginBottom: 16 }}>Bạn đang đặt với tư cách <b>khách vãng lai</b>. <Link to={`/dang-nhap?next=/dat-tour/${dotId}`}>Đăng nhập</Link> để quản lý đơn dễ hơn.</div>}

      <form onSubmit={submit} className="grid grid-main">
        <div className="stack">
          <div className="card">
            <h3>Thông tin người đặt</h3>
            <div className="grid grid-3">
              <div className="field"><label>Họ tên *</label><input required value={form.contactName} onChange={(e) => setForm({ ...form, contactName: e.target.value })} /></div>
              <div className="field"><label>Số điện thoại *</label><input required pattern="[0-9+]{9,15}" value={form.contactPhone} onChange={(e) => setForm({ ...form, contactPhone: e.target.value })} /></div>
              <div className="field"><label>Email</label><input type="email" value={form.contactEmail} onChange={(e) => setForm({ ...form, contactEmail: e.target.value })} /></div>
            </div>
            <div className="field"><label>Ghi chú</label><textarea value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></div>
          </div>

          <div className="card">
            <div className="row between"><h3>Danh sách hành khách ({pax.length})</h3>
              <button type="button" className="btn secondary sm" onClick={() => setPax([...pax, emptyPax()])}>+ Thêm hành khách</button></div>
            <p className="muted small">Loại khách và giá được xác định theo tuổi tại ngày khởi hành. Mỗi đơn cần ít nhất một hành khách từ 18 tuổi.</p>
            {pax.map((p, i) => (
              <div key={i} className="box" style={{ marginBottom: 10 }}>
                <div className="row between"><strong>Hành khách {i + 1}</strong>
                  {pax.length > 1 && <button type="button" className="btn link small" onClick={() => setPax(pax.filter((_, j) => j !== i))}>Xóa</button>}</div>
                <div className="grid grid-3">
                  <div className="field"><label>Họ tên *</label><input required value={p.fullName} onChange={(e) => setP(i, 'fullName', e.target.value)} /></div>
                  <div className="field"><label>Ngày sinh *</label><input required type="date" max={new Date().toISOString().slice(0, 10)} value={p.birthDate} onChange={(e) => setP(i, 'birthDate', e.target.value)} /></div>
                  <div className="field"><label>Giới tính</label>
                    <select value={p.gender} onChange={(e) => setP(i, 'gender', e.target.value)}><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option></select></div>
                  <div className="field"><label>Số điện thoại</label><input value={p.phone} onChange={(e) => setP(i, 'phone', e.target.value)} /></div>
                  <div className="field"><label>CCCD / Hộ chiếu</label><input value={p.idNumber} onChange={(e) => setP(i, 'idNumber', e.target.value)} /></div>
                  <div className="field"><label>Điểm đón</label>
                    <select value={p.pickupPointId} onChange={(e) => setP(i, 'pickupPointId', e.target.value)}>
                      <option value="">— Tự đến điểm tập trung —</option>
                      {dot.pickups.map((d) => <option key={d.pickupPointId} value={d.pickupPointId}>{d.name} ({hhmm(d.pickupTime)}){d.fee > 0 ? ` +${money(d.fee)}` : ''}</option>)}
                    </select></div>
                </div>
              </div>
            ))}
          </div>

          {tour.surcharges.length > 0 && (
            <div className="card">
              <h3>Phụ thu tùy chọn</h3>
              {tour.surcharges.map((p) => (
                <div key={p.id} className="row between" style={{ padding: '6px 0' }}>
                  <label className="check" style={{ margin: 0 }}>
                    <input type="checkbox" checked={p.id in phuThu}
                      onChange={(e) => { const n = { ...phuThu }; if (e.target.checked) n[p.id] = 1; else delete n[p.id]; setPhuThu(n) }} />
                    {p.name} <span className="muted small">({CACH_TINH_PHU_THU[p.method]} · {money(p.unitPrice)})</span>
                  </label>
                  {p.method === 'PER_UNIT' && p.id in phuThu && (
                    <input type="number" min="1" style={{ width: 90 }} value={phuThu[p.id]} onChange={(e) => setPhuThu({ ...phuThu, [p.id]: e.target.value })} />
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <aside>
          <div className="card" style={{ position: 'sticky', top: 80 }}>
            <h3>Tạm tính</h3>
            {quoteErr && <Alert>{quoteErr}</Alert>}
            {!quote ? <p className="muted small">Nhập họ tên và ngày sinh của hành khách để xem giá.</p> : (
              <>
                {quote.passengers.map((h, i) => (
                  <div key={i} className="summary-line small"><span>{h.fullName} <span className="muted">({h.passengerTypeName}, {h.age}t)</span></span><span>{money(h.price)}</span></div>
                ))}
                {quote.pickupTotal > 0 && <div className="summary-line small"><span>Phí điểm đón</span><span>{money(quote.pickupTotal)}</span></div>}
                {quote.surcharges.map((l) => <div key={l.surchargeId} className="summary-line small"><span>{l.name} × {l.quantity}</span><span>{money(l.amount)}</span></div>)}
                <div className="summary-line total"><span>Tổng cộng</span><span className="price">{money(quote.totalAmount)}</span></div>
                <div className="muted small">Số chỗ chiếm: {quote.seats} · Đặt cọc tối thiểu: {money(quote.minimumFirstPayment)}</div>
              </>
            )}
            <Alert>{err}</Alert>
            <button className="btn" style={{ width: '100%', marginTop: 14 }} disabled={busy || !quote || !dot.bookable}>{busy ? 'Đang xử lý…' : 'Xác nhận đặt tour'}</button>
            <p className="muted small">Chỗ được giữ 30 phút; vui lòng thanh toán (ít nhất tiền cọc) trong thời hạn giữ chỗ.</p>
          </div>
        </aside>
      </form>
    </div>
  )
}
