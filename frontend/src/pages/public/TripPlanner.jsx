import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { errMsg, money, hhmm } from '../../api'
import { useAuth } from '../../auth.jsx'
import { Alert, Loading, TourCard } from '../../components/ui.jsx'
import { tenKhuVuc } from '../../tourUtils.js'

const NHIP = [
  ['RELAXED', 'Thong thả', '8:00 – 16:00'],
  ['MODERATE', 'Vừa phải', '7:30 – 17:30'],
  ['PACKED', 'Đi dày', '7:00 – 19:00'],
]
const UOC_TINH = ['ESTIMATED', 'UNVERIFIED', 'UNKNOWN']
const tomorrow = () => { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10) }
const thu = (v) => new Date(v).toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit' })
const phut = (m) => (m >= 60 ? `${Math.floor(m / 60)} giờ ${m % 60 ? `${m % 60} phút` : ''}` : `${m} phút`)

/**
 * Gợi ý lịch trình tự túc: khách chọn ngày đi, số ngày, nhịp đi, nơi lưu trú, số người, ngân sách, sở thích, khu vực.
 * Máy chủ xếp lịch bằng thuật toán Iterated Local Search và gợi ý các tour đang bán giống nhất.
 * Khách đã đăng nhập lưu được lịch trình; ?trip=<id> mở lại một lịch trình đã lưu.
 */
export default function TripPlanner() {
  const { user } = useAuth()
  const [sp] = useSearchParams()
  const [lodgings, setLodgings] = useState([])
  const [regions, setRegions] = useState([])
  const [categories, setCategories] = useState([])
  const [f, setF] = useState({ startDate: tomorrow(), days: 2, pace: 'MODERATE', lodging: 'VINH_LONG_CENTER', adults: 2, children: 0, budget: '', categoryIds: [], regionIds: [] })
  const [plan, setPlan] = useState(null)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [saveMsg, setSaveMsg] = useState('')
  const [title, setTitle] = useState('')

  useEffect(() => {
    api.get('/api/v1/itineraries/lodging-presets').then((r) => setLodgings(r.data)).catch(() => {})
    api.get('/api/v1/regions').then((r) => setRegions(r.data)).catch(() => {})
    api.get('/api/v1/tour-categories').then((r) => setCategories(r.data)).catch(() => {})
  }, [])

  // Mở lại lịch trình đã lưu
  useEffect(() => {
    const trip = sp.get('trip')
    if (!trip || !user) return
    api.get(`/api/v1/me/itineraries/${trip}`).then(({ data }) => {
      const r = data.request
      setF({ ...r, budget: r.budget ?? '', children: r.children ?? 0, categoryIds: r.categoryIds || [], regionIds: r.regionIds || [], lodging: r.lodging || 'VINH_LONG_CENTER' })
      setPlan(data.plan); setTitle(data.title)
    }).catch((e) => setErr(errMsg(e)))
  }, [sp, user])

  const toggle = (k, v) => setF({ ...f, [k]: f[k].includes(v) ? f[k].filter((x) => x !== v) : [...f[k], v] })
  const request = () => ({
    startDate: f.startDate, days: Number(f.days), pace: f.pace, lodging: f.lodging, adults: Number(f.adults), children: Number(f.children) || 0,
    budget: f.budget === '' ? null : Number(f.budget), categoryIds: f.categoryIds, regionIds: f.regionIds,
  })

  const suggest = async (e) => {
    e.preventDefault()
    setBusy(true); setErr(''); setSaveMsg('')
    try {
      setPlan((await api.post('/api/v1/itineraries/suggest', request())).data)
      setTitle(`Lịch trình ${f.days} ngày từ ${new Date(f.startDate).toLocaleDateString('vi-VN')}`)
    } catch (x) { setPlan(null); setErr(errMsg(x)) } finally { setBusy(false) }
  }
  const save = async () => {
    setSaveMsg(''); setErr('')
    try { await api.post('/api/v1/itineraries', { title: title || 'Lịch trình của tôi', request: request() }); setSaveMsg('Đã lưu vào tài khoản của bạn.') } catch (x) { setErr(errMsg(x)) }
  }

  return (
    <div className="container section">
      <h1>Gợi ý lịch trình</h1>
      <p className="muted">Chọn số ngày, nhịp đi, nơi lưu trú và sở thích — hệ thống tự xếp các điểm đến thành lịch trình từng ngày có giờ cụ thể, đúng giờ mở cửa, có bữa trưa và không vượt ngân sách.</p>

      <div className="grid grid-aside" style={{ alignItems: 'start' }}>
        <form className="card stack" onSubmit={suggest}>
          <div className="grid grid-2">
            <div className="field"><label>Ngày đi</label><input type="date" required min={new Date().toISOString().slice(0, 10)} value={f.startDate} onChange={(e) => setF({ ...f, startDate: e.target.value })} /></div>
            <div className="field"><label>Số ngày</label><select value={f.days} onChange={(e) => setF({ ...f, days: e.target.value })}>{[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n} ngày</option>)}</select></div>
            <div className="field"><label>Người lớn</label><input type="number" min="1" max="50" required value={f.adults} onChange={(e) => setF({ ...f, adults: e.target.value })} /></div>
            <div className="field"><label>Trẻ em</label><input type="number" min="0" max="50" value={f.children} onChange={(e) => setF({ ...f, children: e.target.value })} /></div>
          </div>
          <div className="field"><label>Nhịp đi</label>
            {NHIP.map(([k, l, gio]) => <label key={k} className="check"><input type="radio" checked={f.pace === k} onChange={() => setF({ ...f, pace: k })} /> {l} <span className="muted small">({gio})</span></label>)}</div>
          <div className="field"><label>Nơi lưu trú (xuất phát và trở về mỗi ngày)</label>
            <select value={f.lodging} onChange={(e) => setF({ ...f, lodging: e.target.value })}>{lodgings.map((l) => <option key={l.code} value={l.code}>{l.name}</option>)}</select></div>
          <div className="field"><label>Ngân sách cả nhóm (vé + ăn trưa, để trống = không giới hạn)</label>
            <input type="number" min="0" step="10000" value={f.budget} onChange={(e) => setF({ ...f, budget: e.target.value })} placeholder="VD: 1000000" /></div>
          <div className="field"><label>Sở thích</label>
            <div className="row" style={{ flexWrap: 'wrap' }}>{categories.map((c) => <label key={c.id} className="check"><input type="checkbox" checked={f.categoryIds.includes(c.id)} onChange={() => toggle('categoryIds', c.id)} />{c.name}</label>)}</div></div>
          <div className="field"><label>Khu vực ưu tiên</label>
            <div className="row" style={{ flexWrap: 'wrap' }}>{regions.map((r) => <label key={r.id} className="check"><input type="checkbox" checked={f.regionIds.includes(r.id)} onChange={() => toggle('regionIds', r.id)} />{tenKhuVuc(r.name)}</label>)}</div></div>
          <button className="btn" disabled={busy}>{busy ? 'Đang xếp lịch…' : 'Gợi ý lịch trình'}</button>
        </form>

        <div className="stack">
          <Alert>{err}</Alert>
          {busy && <Loading />}
          {!plan && !busy && <div className="card muted">Kết quả sẽ hiện ở đây sau khi bạn bấm “Gợi ý lịch trình”.</div>}
          {plan && !busy && <PlanView plan={plan} />}
          {plan && !busy && (
            <div className="card">
              {user ? (
                <div className="row">
                  <input className="grow" style={{ width: 'auto' }} value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Tên lịch trình" />
                  <button className="btn secondary" onClick={save}>Lưu lịch trình</button>
                </div>
              ) : <p className="small" style={{ margin: 0 }}><Link to="/dang-nhap?next=/goi-y-lich-trinh">Đăng nhập</Link> để lưu lịch trình và xem lại sau.</p>}
              <Alert type="ok">{saveMsg}</Alert>
            </div>
          )}
          {plan?.similarTours?.length > 0 && !busy && (
            <>
              <h2 style={{ margin: '12px 0 0' }}>Tour có sẵn gần giống lịch trình này</h2>
              <div className="grid grid-2">{plan.similarTours.map((s) => <TourCard key={s.tour.id} t={s.tour} />)}</div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function PlanView({ plan }) {
  return (
    <>
      <div className="card">
        <div className="grid grid-4 small">
          <div><div className="muted">Nơi lưu trú</div><b>{plan.lodging?.name || 'Vị trí tự chọn'}</b></div>
          <div><div className="muted">Chi phí ước tính</div><b className="price">{money(plan.estimatedCost)}</b></div>
          <div><div className="muted">Tổng thời gian đi đường</div><b>{phut(plan.totalTravelMinutes)}</b></div>
          <div><div className="muted">Số khách</div><b>{plan.adults} người lớn{plan.children ? `, ${plan.children} trẻ em` : ''}</b></div>
        </div>
      </div>
      {plan.dayPlans.map((d) => (
        <div key={d.dayNumber} className="card">
          <div className="row between"><h3 style={{ margin: 0 }}>Ngày {d.dayNumber} · {thu(d.date)}</h3><span className="muted small">{money(d.cost)} · đi đường {phut(d.travelMinutes)}</span></div>
          <div className="timeline" style={{ marginTop: 10 }}>
            <div className="act"><div className="muted small">{hhmm(d.leaveLodgingAt)}</div><div>Rời nơi lưu trú</div></div>
            {d.stops.map((s, i) => (
              <div key={i} className="act">
                <div className="muted small">{hhmm(s.start)}–{hhmm(s.leave)}</div>
                <div>
                  {s.type === 'LUNCH' ? <b>🍽 Ăn trưa{s.name ? ` gần ${s.name}` : ''}</b> : <b>📍 {s.name}</b>}
                  <div className="small muted">
                    Đi đường {s.travelMinutes} phút{s.waitMinutes > 0 && ` · chờ mở cửa ${s.waitMinutes} phút`}
                    {s.cost > 0 && ` · ${money(s.cost)}`}
                    {s.type === 'VISIT' && (UOC_TINH.includes(s.hoursQuality) || UOC_TINH.includes(s.priceQuality)) && ' · giờ mở cửa / giá vé là ước tính'}
                  </div>
                </div>
              </div>
            ))}
            <div className="act"><div className="muted small">{hhmm(d.returnAt)}</div><div>Về nơi lưu trú <span className="muted small">(đi đường {d.returnTravelMinutes} phút)</span></div></div>
          </div>
        </div>
      ))}
      <p className="muted small">Thời gian di chuyển được ước tính theo khoảng cách; vui lòng kiểm tra lại giờ mở cửa trước khi đi.</p>
    </>
  )
}
