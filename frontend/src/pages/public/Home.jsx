import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { fmtDate, imgUrl } from '../../api'
import { TourCard, Loading } from '../../components/ui.jsx'

export default function Home() {
  const [noiBat, setNoiBat] = useState(null)
  const [moiNhat, setMoiNhat] = useState([])
  const [tin, setTin] = useState([])
  const [dm, setDm] = useState({ loaiHinhs: [], khuVucs: [] })
  const [f, setF] = useState({ q: '', khuVucId: '', loaiHinhId: '' })
  const nav = useNavigate()

  useEffect(() => {
    api.get('/api/public/tours', { params: { noiBat: true, size: 6 } }).then((r) => setNoiBat(r.data.content)).catch(() => setNoiBat([]))
    api.get('/api/public/tours', { params: { size: 6 } }).then((r) => setMoiNhat(r.data.content)).catch(() => {})
    api.get('/api/public/tin-tuc', { params: { size: 3 } }).then((r) => setTin(r.data.content)).catch(() => {})
    api.get('/api/public/danh-muc').then((r) => setDm(r.data)).catch(() => {})
  }, [])

  const search = (e) => {
    e.preventDefault()
    const p = new URLSearchParams(Object.entries(f).filter(([, v]) => v))
    nav(`/dich-vu?${p}`)
  }

  return (
    <>
      <section className="hero">
        <div className="container">
          <h1>Khám phá Vĩnh Long – miệt vườn, xứ dừa và văn hóa Khmer</h1>
          <p>Tour trọn gói trên địa bàn tỉnh Vĩnh Long (mới) gồm các khu vực Vĩnh Long, Bến Tre, Trà Vinh. Đặt chỗ trực tuyến, thanh toán qua VNPay.</p>
          <form className="search-bar" onSubmit={search}>
            <input placeholder="Tìm tên tour, mã tour…" value={f.q} onChange={(e) => setF({ ...f, q: e.target.value })} />
            <select value={f.khuVucId} onChange={(e) => setF({ ...f, khuVucId: e.target.value })} aria-label="Khu vực">
              <option value="">Mọi khu vực</option>
              {dm.khuVucs.map((k) => <option key={k.id} value={k.id}>{k.ten}</option>)}
            </select>
            <select value={f.loaiHinhId} onChange={(e) => setF({ ...f, loaiHinhId: e.target.value })} aria-label="Loại hình">
              <option value="">Mọi loại hình</option>
              {dm.loaiHinhs.map((k) => <option key={k.id} value={k.id}>{k.ten}</option>)}
            </select>
            <button className="btn sun">Tìm tour</button>
          </form>
        </div>
      </section>

      <section className="section container">
        <div className="section-title"><h2>Tour nổi bật</h2><Link to="/dich-vu">Xem tất cả →</Link></div>
        {noiBat === null ? <Loading /> : (
          <div className="grid grid-3">{(noiBat.length ? noiBat : moiNhat).map((t) => <TourCard key={t.tourId} t={t} />)}</div>
        )}
      </section>

      <section className="section container">
        <div className="grid grid-3">
          {[
            ['🛶', 'Miệt vườn sông nước', 'Đi xuồng kênh rạch, vườn trái cây, nhà cổ.'],
            ['🥥', 'Xứ dừa', 'Làng nghề kẹo dừa, cồn nổi trên sông Tiền.'],
            ['🛕', 'Văn hóa Khmer', 'Chùa Khmer cổ, lễ hội và ẩm thực đặc sắc.'],
          ].map(([i, t, d]) => (
            <div key={t} className="card"><div style={{ fontSize: 30 }}>{i}</div><h3>{t}</h3><p className="muted">{d}</p></div>
          ))}
        </div>
      </section>

      {tin.length > 0 && (
        <section className="section container">
          <div className="section-title"><h2>Tin tức</h2><Link to="/tin-tuc">Xem tất cả →</Link></div>
          <div className="grid grid-3">
            {tin.map((n) => (
              <Link key={n.tinTucId} to={`/tin-tuc/${n.slug}`} className="card flush" style={{ color: 'inherit' }}>
                <div style={{ aspectRatio: '16/9', background: `var(--green-soft) center/cover url(${imgUrl(n.anhBia) || ''})` }} />
                <div style={{ padding: 16 }}>
                  <div className="muted small">{fmtDate(n.ngayDang)}</div>
                  <h3 style={{ fontSize: '1.05rem' }}>{n.tieuDe}</h3>
                  <p className="muted small">{n.tomTat}</p>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}
    </>
  )
}
