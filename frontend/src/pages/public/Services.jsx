import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import api, { errMsg } from '../../api'
import { TourCard, Pagination, Loading, Alert } from '../../components/ui.jsx'

/** Mục "Dịch vụ": danh sách tour có tìm kiếm/lọc (Q02, Q03, Q07) */
export default function Services() {
  const [sp, setSp] = useSearchParams()
  const [dm, setDm] = useState({ loaiHinhs: [], khuVucs: [], dichVus: [] })
  const [data, setData] = useState(null)
  const [err, setErr] = useState('')
  const [q, setQ] = useState(sp.get('q') || '')

  const params = Object.fromEntries(sp.entries())
  const page = Number(sp.get('page') || 0)

  useEffect(() => { api.get('/api/public/danh-muc').then((r) => setDm(r.data)).catch(() => {}) }, [])

  useEffect(() => {
    setData(null)
    api.get('/api/public/tours', { params: { ...Object.fromEntries(sp.entries()), size: 9 } })
      .then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  }, [sp])

  const set = (k, v) => {
    const n = { ...params, [k]: v }
    if (!v) delete n[k]
    if (k !== 'page') delete n.page
    setSp(n)
  }

  return (
    <>
      <div className="page-head"><div className="container"><h1>Dịch vụ – Tour du lịch</h1></div></div>
      <div className="container grid" style={{ gridTemplateColumns: '260px 1fr' }}>
        <aside className="card" style={{ alignSelf: 'start' }}>
          <form onSubmit={(e) => { e.preventDefault(); set('q', q) }} className="field">
            <label>Từ khóa</label>
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Tên hoặc mã tour" />
          </form>
          <div className="field">
            <label>Khu vực</label>
            <select value={params.khuVucId || ''} onChange={(e) => set('khuVucId', e.target.value)}>
              <option value="">Tất cả</option>
              {dm.khuVucs.map((x) => <option key={x.id} value={x.id}>{x.ten}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Loại hình</label>
            <select value={params.loaiHinhId || ''} onChange={(e) => set('loaiHinhId', e.target.value)}>
              <option value="">Tất cả</option>
              {dm.loaiHinhs.map((x) => <option key={x.id} value={x.id}>{x.ten}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Có bao gồm dịch vụ</label>
            <select value={params.dichVuId || ''} onChange={(e) => set('dichVuId', e.target.value)}>
              <option value="">Không lọc</option>
              {dm.dichVus.map((x) => <option key={x.id} value={x.id}>{x.ten}</option>)}
            </select>
          </div>
          <button className="btn secondary sm" onClick={() => { setQ(''); setSp({}) }}>Xóa bộ lọc</button>
        </aside>
        <div>
          <Alert>{err}</Alert>
          {!data ? <Loading /> : data.content.length === 0 ? <p className="muted">Không tìm thấy tour phù hợp.</p> : (
            <>
              <p className="muted small">Tìm thấy {data.totalElements} tour</p>
              <div className="grid grid-3">{data.content.map((t) => <TourCard key={t.tourId} t={t} />)}</div>
              <Pagination page={page} totalPages={data.totalPages} onChange={(p) => set('page', String(p))} />
            </>
          )}
        </div>
      </div>
    </>
  )
}
