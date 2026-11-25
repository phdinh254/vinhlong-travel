import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import api, { errMsg, imgUrl, money, fmtDateTime, nhanDot, CACH_TINH_PHU_THU } from '../../api'
import { Loading, Alert } from '../../components/ui.jsx'
import Itinerary from '../../components/Itinerary.jsx'
import ImageCarousel from '../../components/ImageCarousel.jsx'
import { thoiLuong, tenMocPhat } from '../../tourUtils.js'

export default function TourDetail() {
  const { id } = useParams()
  const [t, setT] = useState(null)
  const [dots, setDots] = useState([])
  const [policy, setPolicy] = useState(null)
  const [err, setErr] = useState('')
  const [dotXem, setDotXem] = useState(null)
  const [lichTrinhDot, setLichTrinhDot] = useState(null)

  useEffect(() => {
    api.get(`/api/v1/tours/${id}`).then((r) => setT(r.data)).catch((e) => setErr(errMsg(e)))
    api.get(`/api/v1/tours/${id}/departures`).then((r) => setDots(r.data)).catch(() => setDots([]))
    api.get('/api/v1/cancellation-policies/current').then((r) => setPolicy(r.data)).catch(() => {})
  }, [id])

  // Nút "Đặt tour" ở thẻ tour dẫn tới #lich-khoi-hanh: cuộn tới khi dữ liệu đã tải
  useEffect(() => {
    if (t && window.location.hash === '#lich-khoi-hanh') document.getElementById('lich-khoi-hanh')?.scrollIntoView({ behavior: 'smooth' })
  }, [t])

  const xemLichTrinhDot = async (d) => {
    setDotXem(d); setLichTrinhDot(null)
    try { setLichTrinhDot((await api.get(`/api/v1/departures/${d.id}/itinerary`)).data.days) } catch (e) { setErr(errMsg(e)) }
  }

  if (err && !t) return <div className="container section"><Alert>{err}</Alert></div>
  if (!t) return <div className="container section"><Loading /></div>

  // Ảnh đại diện lên đầu, các ảnh còn lại giữ thứ tự admin sắp xếp
  const anh = [...t.images].sort((a, b) => Number(b.cover) - Number(a.cover)).map((h) => ({ src: imgUrl(h.url), caption: h.caption }))

  return (
    <div className="container section">
      <div className="chips" style={{ marginBottom: 8 }}>
        {t.regions.map((k) => <span key={k.id} className="badge info">{k.name}</span>)}
        {t.categories.map((k) => <span key={k.id} className="badge">{k.name}</span>)}
      </div>
      <h1>{t.name}</h1>
      <p className="muted">Mã tour: {t.code} · Thời lượng: {thoiLuong(t)}</p>

      <ImageCarousel label={`Ảnh tour ${t.name}`} images={anh} />

      <div className="grid grid-main" style={{ marginTop: 24 }}>
        <div className="stack">
          <div className="card"><h2>Giới thiệu tour</h2><div className="prose" dangerouslySetInnerHTML={{ __html: t.description || t.summary || '' }} /></div>
          <div className="card"><h2>Lịch trình</h2><Itinerary days={t.itinerary?.days} /></div>
          <div className="card grid grid-2">
            <div><h3>✅ Bao gồm</h3><ul>{t.includedAmenities.map((d) => <li key={d.id}>{d.name}</li>)}</ul></div>
            <div><h3>❌ Không bao gồm</h3><ul>{t.excludedAmenities.map((d) => <li key={d.id}>{d.name}</li>)}</ul></div>
          </div>
          {policy && (
            <div className="card">
              <h2>Chính sách hủy</h2>
              <p className="muted small">{policy.name} (áp dụng từ {new Date(policy.effectiveFrom).toLocaleDateString('vi-VN')}). Chính sách được chốt vào đơn tại thời điểm đặt.</p>
              <table><thead><tr><th>Hủy trước ngày khởi hành</th><th>Phí phạt</th></tr></thead><tbody>
                {policy.tiers.map((m, i) => <tr key={i}><td>{tenMocPhat(m)}</td><td>{m.penaltyPercent}%</td></tr>)}
              </tbody></table>
            </div>
          )}
        </div>

        <aside className="stack">
          <div className="card">
            <h3 id="lich-khoi-hanh" style={{ scrollMarginTop: 90 }}>Lịch khởi hành</h3>
            {dots.length === 0 && <p className="muted">Hiện chưa có đợt khởi hành mở bán.</p>}
            <p className="small" style={{ margin: '0 0 12px' }}>
              Cần tư vấn hoặc muốn đi theo nhóm riêng? <Link to={`/lien-he?tourId=${t.id}`}>Hỏi về tour này →</Link>
            </p>
            {dots.map((d) => {
              const [label, cls] = nhanDot(d)
              return (
                <div key={d.id} className="box" style={{ marginBottom: 10 }}>
                  <div className="row between">
                    <strong>{fmtDateTime(d.departureAt)}</strong>
                    <span className={`badge ${cls}`}>{label}</span>
                  </div>
                  <div className="small muted">Về: {fmtDateTime(d.returnAt)} · Còn {d.seatsLeft}/{d.capacity} chỗ</div>
                  <div className="small" style={{ margin: '6px 0' }}>
                    {d.prices.map((g) => (
                      <div key={g.passengerTypeId} className="row between">
                        <span>{g.passengerTypeName} ({g.minAge}–{g.maxAge >= 100 ? '…' : g.maxAge} tuổi)</span><strong>{money(g.price)}</strong>
                      </div>
                    ))}
                  </div>
                  <div className="row">
                    {d.bookable
                      ? <Link className="btn sm" to={`/dat-tour/${d.id}`}>Đặt tour</Link>
                      : <button className="btn sm" disabled>{label}</button>}
                    <button className="btn link small" onClick={() => xemLichTrinhDot(d)}>Lịch trình của đợt</button>
                  </div>
                </div>
              )
            })}
          </div>
          {t.surcharges.length > 0 && (
            <div className="card">
              <h3>Phụ thu tùy chọn</h3>
              {t.surcharges.map((p) => <div key={p.id} className="summary-line small"><span>{p.name} <span className="muted">({CACH_TINH_PHU_THU[p.method]})</span></span><span>{money(p.unitPrice)}</span></div>)}
            </div>
          )}
        </aside>
      </div>

      {dotXem && (
        <div className="modal-bg" onMouseDown={(e) => e.target === e.currentTarget && setDotXem(null)}>
          <div className="modal">
            <div className="row between"><h3>Lịch trình đợt {dotXem.code}</h3><button className="btn link" onClick={() => setDotXem(null)}>✕</button></div>
            {lichTrinhDot ? <Itinerary days={lichTrinhDot} /> : <Loading />}
          </div>
        </div>
      )}
    </div>
  )
}
