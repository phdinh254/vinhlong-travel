import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime, CACH_TINH_PHU_THU } from '../../api'
import { useAuth } from '../../auth.jsx'
import { Alert, Loading } from '../../components/ui.jsx'

const emptyPax = () => ({ hoTen: '', ngaySinh: '', gioiTinh: 'NAM', soDienThoai: '', cccdPassport: '', diemDonId: '' })

/** Đặt tour: khách đăng nhập hoặc khách vãng lai (Q22–Q29) */
export default function Booking() {
  const { dotId } = useParams()
  const { user } = useAuth()
  const nav = useNavigate()
  const [dot, setDot] = useState(null)
  const [tour, setTour] = useState(null)
  const [form, setForm] = useState({ tenNguoiDat: user?.hoTen || '', sdtNguoiDat: user?.soDienThoai || '', emailNguoiDat: user?.email || '', ghiChu: '' })
  const [pax, setPax] = useState([{ ...emptyPax(), hoTen: user?.hoTen || '' }])
  const [phuThu, setPhuThu] = useState({}) // phuThuId -> soLuong
  const [quote, setQuote] = useState(null)
  const [quoteErr, setQuoteErr] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.get(`/api/public/dots/${dotId}`).then(async (r) => {
      setDot(r.data)
      setTour((await api.get(`/api/public/tours/${r.data.tourId}`)).data)
    }).catch((e) => setErr(errMsg(e)))
  }, [dotId])

  const body = () => ({
    dotKhoiHanhId: Number(dotId),
    ...form,
    emailNguoiDat: form.emailNguoiDat || null,
    hanhKhachs: pax.map((p) => ({ ...p, diemDonId: p.diemDonId ? Number(p.diemDonId) : null, gioiTinh: p.gioiTinh || null })),
    phuThus: Object.entries(phuThu).map(([id, sl]) => ({ phuThuId: Number(id), soLuong: Number(sl) || 1 })),
  })

  // Báo giá tự động khi thông tin hành khách đủ ngày sinh
  useEffect(() => {
    if (!dot || pax.some((p) => !p.ngaySinh || !p.hoTen)) { setQuote(null); return }
    const h = setTimeout(() => {
      api.post('/api/bookings/bao-gia', { ...body(), tenNguoiDat: form.tenNguoiDat || 'x', sdtNguoiDat: form.sdtNguoiDat || '000000000' })
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
      const { data } = await api.post('/api/bookings', body())
      nav(`/don/${data.maDon}?sdt=${encodeURIComponent(data.sdtNguoiDat)}`, { state: { moi: true } })
    } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }

  if (err && !dot) return <div className="container section"><Alert>{err}</Alert></div>
  if (!dot || !tour) return <div className="container section"><Loading /></div>

  return (
    <div className="container section">
      <p><Link to={`/tour/${tour.tourId}`}>← {tour.tenTour}</Link></p>
      <h1>Đặt tour</h1>
      <p className="muted">Khởi hành {fmtDateTime(dot.ngayGioDi)} · Còn {dot.soChoConLai} chỗ · Mã đợt {dot.maDot}</p>
      {!user && <div className="alert" style={{ marginBottom: 16 }}>Bạn đang đặt với tư cách <b>khách vãng lai</b>. <Link to={`/dang-nhap?next=/dat-tour/${dotId}`}>Đăng nhập</Link> để quản lý đơn dễ hơn.</div>}

      <form onSubmit={submit} className="grid" style={{ gridTemplateColumns: '2fr 1fr' }}>
        <div className="stack">
          <div className="card">
            <h3>Thông tin người đặt</h3>
            <div className="grid grid-3">
              <div className="field"><label>Họ tên *</label><input required value={form.tenNguoiDat} onChange={(e) => setForm({ ...form, tenNguoiDat: e.target.value })} /></div>
              <div className="field"><label>Số điện thoại *</label><input required pattern="[0-9+]{9,15}" value={form.sdtNguoiDat} onChange={(e) => setForm({ ...form, sdtNguoiDat: e.target.value })} /></div>
              <div className="field"><label>Email</label><input type="email" value={form.emailNguoiDat} onChange={(e) => setForm({ ...form, emailNguoiDat: e.target.value })} /></div>
            </div>
            <div className="field"><label>Ghi chú</label><textarea value={form.ghiChu} onChange={(e) => setForm({ ...form, ghiChu: e.target.value })} /></div>
          </div>

          <div className="card">
            <div className="row between"><h3>Danh sách hành khách ({pax.length})</h3>
              <button type="button" className="btn secondary sm" onClick={() => setPax([...pax, emptyPax()])}>+ Thêm hành khách</button></div>
            <p className="muted small">Loại khách và giá được xác định theo tuổi tại ngày khởi hành. Em bé (dưới 5 tuổi) không chiếm ghế riêng.</p>
            {pax.map((p, i) => (
              <div key={i} className="box" style={{ marginBottom: 10 }}>
                <div className="row between"><strong>Hành khách {i + 1}</strong>
                  {pax.length > 1 && <button type="button" className="btn link small" onClick={() => setPax(pax.filter((_, j) => j !== i))}>Xóa</button>}</div>
                <div className="grid grid-3">
                  <div className="field"><label>Họ tên *</label><input required value={p.hoTen} onChange={(e) => setP(i, 'hoTen', e.target.value)} /></div>
                  <div className="field"><label>Ngày sinh *</label><input required type="date" max={new Date().toISOString().slice(0, 10)} value={p.ngaySinh} onChange={(e) => setP(i, 'ngaySinh', e.target.value)} /></div>
                  <div className="field"><label>Giới tính</label>
                    <select value={p.gioiTinh} onChange={(e) => setP(i, 'gioiTinh', e.target.value)}><option value="NAM">Nam</option><option value="NU">Nữ</option><option value="KHAC">Khác</option></select></div>
                  <div className="field"><label>Số điện thoại</label><input value={p.soDienThoai} onChange={(e) => setP(i, 'soDienThoai', e.target.value)} /></div>
                  <div className="field"><label>CCCD / Hộ chiếu</label><input value={p.cccdPassport} onChange={(e) => setP(i, 'cccdPassport', e.target.value)} /></div>
                  <div className="field"><label>Điểm đón</label>
                    <select value={p.diemDonId} onChange={(e) => setP(i, 'diemDonId', e.target.value)}>
                      <option value="">— Tự đến điểm tập trung —</option>
                      {dot.diemDons.map((d) => <option key={d.diemDonId} value={d.diemDonId}>{d.tenDiemDon} ({fmtDateTime(d.gioDon)}){Number(d.phiPhuThuDon) > 0 ? ` +${money(d.phiPhuThuDon)}` : ''}</option>)}
                    </select></div>
                </div>
              </div>
            ))}
          </div>

          {tour.phuThus.length > 0 && (
            <div className="card">
              <h3>Phụ thu tùy chọn</h3>
              {tour.phuThus.map((p) => (
                <div key={p.phuThuId} className="row between" style={{ padding: '6px 0' }}>
                  <label className="check" style={{ margin: 0 }}>
                    <input type="checkbox" checked={p.phuThuId in phuThu}
                      onChange={(e) => { const n = { ...phuThu }; if (e.target.checked) n[p.phuThuId] = 1; else delete n[p.phuThuId]; setPhuThu(n) }} />
                    {p.tenPhuThu} <span className="muted small">({CACH_TINH_PHU_THU[p.cachTinh]} · {money(p.donGia)})</span>
                  </label>
                  {p.cachTinh === 'THEO_SO_LUONG' && p.phuThuId in phuThu && (
                    <input type="number" min="1" style={{ width: 90 }} value={phuThu[p.phuThuId]} onChange={(e) => setPhuThu({ ...phuThu, [p.phuThuId]: e.target.value })} />
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
                {quote.hanhKhachs.map((h, i) => (
                  <div key={i} className="summary-line small"><span>{h.hoTen} <span className="muted">({h.tenLoaiKhach}, {h.tuoi}t)</span></span><span>{money(h.giaApDung)}</span></div>
                ))}
                {Number(quote.tongPhiDon) > 0 && <div className="summary-line small"><span>Phụ thu điểm đón</span><span>{money(quote.tongPhiDon)}</span></div>}
                {quote.phuThus.map((l) => <div key={l.phuThuId} className="summary-line small"><span>{l.tenPhuThu} × {l.soLuong}</span><span>{money(l.thanhTien)}</span></div>)}
                <div className="summary-line total"><span>Tổng cộng</span><span className="price">{money(quote.tongTien)}</span></div>
                <div className="muted small">Số chỗ chiếm: {quote.soChoChiem} · Đặt cọc tối thiểu: {money(quote.soTienCocToiThieu)}</div>
              </>
            )}
            <Alert>{err}</Alert>
            <button className="btn" style={{ width: '100%', marginTop: 14 }} disabled={busy || !quote}>{busy ? 'Đang xử lý…' : 'Xác nhận đặt tour'}</button>
            <p className="muted small">Chỗ được giữ tạm thời; vui lòng thanh toán trong thời hạn giữ chỗ.</p>
          </div>
        </aside>
      </form>
    </div>
  )
}
