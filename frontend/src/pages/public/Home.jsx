import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { errMsg, fmtDate, imgUrl, money } from '../../api'
import { TourCard, Loading } from '../../components/ui.jsx'
import { tourImage } from '../../tourUtils.js'
import { tenChuyenMuc, anhMacDinh } from '../../content/chuyenMuc.js'
import { SO_NGAY } from '../../content/timTour.js'
import { DIEM_DEN, TRAI_NGHIEM, QUY_TRINH, SO_LIEU, DANH_GIA, FAQ } from '../../content/trangChu.js'
import { useReveal } from '../../useReveal.js'
import './home.css'

const img = (k) => `/images/${k}.jpg`
const MOSAIC = ['vui-choi', 'song-nuoc', 'con-phung', 'homestay', 'an-binh', 'lang-gom']
const ngayNgan = (v) => new Date(v).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })

/** Tạo link /tour?... từ tên điểm đến / loại hình (so khớp với danh mục trong DB), dự phòng bằng từ khóa */
function tourLink(dm, { diemDen, loaiHinh }) {
  const has = (a, b) => a.toLowerCase().includes(b.toLowerCase())
  if (diemDen) {
    const d = dm.diemDens.find((x) => has(x.tenDiemDen, diemDen))
    return d ? `/tour?diemDenId=${d.diemDenId}` : `/tour?q=${encodeURIComponent(diemDen)}`
  }
  const l = dm.loaiHinhs.find((x) => has(x.ten, loaiHinh))
  return l ? `/tour?loaiHinhId=${l.id}` : '/tour'
}

export default function Home() {
  const [tours, setTours] = useState(null)
  const [tongTour, setTongTour] = useState(0)
  const [tin, setTin] = useState([])
  const [dm, setDm] = useState({ loaiHinhs: [], khuVucs: [], diemDens: [] })
  const [f, setF] = useState({ diemDenId: '', khuVucId: '', loaiHinhId: '', soNgay: '' })
  const nav = useNavigate()
  const root = useReveal()

  useEffect(() => {
    // Tour phổ biến: ưu tiên tour nổi bật, bổ sung tour khác cho đủ 3
    Promise.all([
      api.get('/api/public/tours', { params: { noiBat: true, size: 3 } }),
      api.get('/api/public/tours', { params: { size: 6 } }),
    ]).then(([a, b]) => {
      const list = [...a.data.content]
      for (const t of b.data.content) if (list.length < 3 && !list.some((x) => x.tourId === t.tourId)) list.push(t)
      setTours(list)
      setTongTour(b.data.totalElements)
    }).catch(() => setTours([]))
    api.get('/api/public/tin-tuc', { params: { size: 3 } }).then((r) => setTin(r.data.content)).catch(() => {})
    api.get('/api/public/danh-muc').then((r) => setDm(r.data)).catch(() => {})
  }, [])

  const search = (e) => {
    e.preventDefault()
    const p = new URLSearchParams(Object.entries(f).filter(([, v]) => v))
    nav(`/tour${p.size ? `?${p}` : ''}`)
  }

  const hero = tours?.[0]
  const sapKhoiHanh = tours?.filter((t) => t.ngayKhoiHanh).sort((a, b) => a.ngayKhoiHanh.localeCompare(b.ngayKhoiHanh))[0]

  return (
    <div className="lp" ref={root}>
      {/* ---------- 02 Hero ---------- */}
      <section className="lp-hero">
        <div className="lp-orb a" /><div className="lp-orb b" />
        <div className="container lp-hero-grid">
          <div className="lp-hero-text">
            <h1 className="lp-in" style={{ '--d': '120ms' }}>Khám phá <span className="lp-grad-text">Vĩnh Long</span> theo cách của bạn</h1>
            <p className="lp-in" style={{ '--d': '240ms' }}>Tour trọn gói Vĩnh Long – Bến Tre – Trà Vinh. Chọn lịch, đặt chỗ và thanh toán trực tuyến trong vài phút.</p>
            <div className="lp-cta lp-in" style={{ '--d': '360ms' }}>
              <Link to="/tour" className="lp-btn primary lp-cta-main">Đặt tour ngay →</Link>
            </div>
          </div>

          <div className="lp-visual">
            <div className="lp-photo lp-in" style={{ '--d': '200ms', backgroundImage: `url(${hero ? tourImage(hero) : img('an-binh')})` }} role="img"
              aria-label={hero ? hero.tenTour : 'Cù lao An Bình'} />
            {hero && (
              <Link to={`/tour/${hero.tourId}`} className="lp-float f1 lp-in" style={{ '--d': '520ms' }}>
                <small>{hero.noiBat ? 'Tour nổi bật' : 'Tour gợi ý'}</small>
                <b>{hero.tenTour}</b>
                <span className="lp-float-price">{hero.giaTu ? `Từ ${money(hero.giaTu)}` : 'Liên hệ'}</span>
              </Link>
            )}
            {sapKhoiHanh && (
              <Link to={`/tour/${sapKhoiHanh.tourId}#lich-khoi-hanh`} className="lp-float f2 lp-in" style={{ '--d': '660ms' }}>
                <span className="ic" aria-hidden="true">📅</span>
                <div><b>Khởi hành {ngayNgan(sapKhoiHanh.ngayKhoiHanh)}</b>
                  <small>{sapKhoiHanh.choConLai != null ? `Còn ${sapKhoiHanh.choConLai} chỗ` : 'Đang nhận khách'}</small></div>
              </Link>
            )}
            {tongTour > 0 && (
              <div className="lp-float f3 lp-in" style={{ '--d': '800ms' }}>
                <b>{tongTour} tour</b><small>đang mở bán · 3 khu vực</small>
              </div>
            )}
          </div>
        </div>

        {/* ---------- 03 Tìm tour ---------- */}
        <form className="lp-search lp-in" style={{ '--d': '420ms' }} onSubmit={search} aria-label="Tìm tour">
          <label className="lp-sf"><span>Điểm đến</span>
            <select value={f.diemDenId} onChange={(e) => setF({ ...f, diemDenId: e.target.value })}>
              <option value="">Bạn muốn đi đâu?</option>
              {dm.diemDens.map((d) => <option key={d.diemDenId} value={d.diemDenId}>{d.tenDiemDen}</option>)}
            </select>
          </label>
          <label className="lp-sf"><span>Khu vực</span>
            <select value={f.khuVucId} onChange={(e) => setF({ ...f, khuVucId: e.target.value })}>
              <option value="">Mọi khu vực</option>
              {dm.khuVucs.map((k) => <option key={k.id} value={k.id}>{k.ten.replace(/^Khu vực\s*/i, '')}</option>)}
            </select>
          </label>
          <label className="lp-sf"><span>Loại hình</span>
            <select value={f.loaiHinhId} onChange={(e) => setF({ ...f, loaiHinhId: e.target.value })}>
              <option value="">Mọi loại hình</option>
              {dm.loaiHinhs.map((k) => <option key={k.id} value={k.id}>{k.ten}</option>)}
            </select>
          </label>
          <label className="lp-sf"><span>Thời lượng</span>
            <select value={f.soNgay} onChange={(e) => setF({ ...f, soNgay: e.target.value })}>
              <option value="">Bao lâu cũng được</option>
              {SO_NGAY.map((s) => <option key={s.id} value={s.id}>{s.ten}</option>)}
            </select>
          </label>
          <button className="lp-btn primary">Tìm tour</button>
        </form>
      </section>

      {/* ---------- 04 Điểm đến nổi bật ---------- */}
      <section className="lp-sec container" id="diem-den">
        <Head eyebrow="Điểm đến" title="Điểm đến nổi bật" to="/gioi-thieu" more="Tìm hiểu vùng đất" />
        <div className="lp-bento">
          {DIEM_DEN.map((d) => (
            <Link key={d.k} to={tourLink(dm, d)} className="lp-dest" data-reveal>
              <span className="bg" style={{ backgroundImage: `url(${img(d.k)})` }} aria-hidden="true" />
              <div className="cap">
                <span className="lp-dest-tags"><i>{d.noi}</i><i>{d.loai}</i></span>
                <b>{d.ten}</b>
                <span className="lp-dest-go">Xem tour đi qua đây →</span>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* ---------- 05 Trải nghiệm miền Tây ---------- */}
      <section className="lp-sec container">
        <Head eyebrow="Trải nghiệm" title="Trải nghiệm miền Tây" to="/tour" more="Xem tất cả tour" />
        <div className="lp-exp">
          {TRAI_NGHIEM.map((x) => (
            <Link key={x.k} to={tourLink(dm, x)} className="lp-exp-card" data-reveal>
              <span className="pic" style={{ backgroundImage: `url(${img(x.k)})` }} aria-hidden="true" />
              <span className="txt"><b>{x.ten}</b><small>{x.mo}</small><em>Xem tour →</em></span>
            </Link>
          ))}
        </div>
      </section>

      {/* ---------- 06 Tour phổ biến ---------- */}
      <section className="lp-sec container">
        <Head eyebrow="Tour" title="Tour phổ biến" to="/tour" more="Xem tất cả tour" />
        {tours === null ? <Loading /> : tours.length ? (
          <div className="grid grid-3 lp-tours">{tours.map((t) => <div key={t.tourId} data-reveal><TourCard t={t} /></div>)}</div>
        ) : <p className="muted">Chưa có tour đang mở bán.</p>}
      </section>

      {/* ---------- 07 Quy trình đặt tour ---------- */}
      <section className="lp-sec container">
        <Head eyebrow="Đặt tour dễ dàng" title="Quy trình đặt tour" />
        <ol className="lp-steps">
          {QUY_TRINH.map(([t, d], i) => (
            <li key={t} data-reveal style={{ '--d': `${i * 90}ms` }}>
              <span className="n">{String(i + 1).padStart(2, '0')}</span>
              <h3>{t}</h3>
              <p>{d}</p>
            </li>
          ))}
        </ol>
      </section>

      {/* ---------- 08 Thống kê & hình ảnh ---------- */}
      <section className="lp-proof">
        <div className="container lp-proof-grid">
          <div className="lp-info" data-reveal>
            <span className="lp-chip light">Hành trình đã đồng hành</span>
            <h2>Hành trình được tạo nên từ những trải nghiệm đáng nhớ</h2>
            <div className="lp-stats">
              {SO_LIEU.map(([n, l]) => <div key={l}><b>{n}</b><span>{l}</span></div>)}
            </div>
            <p className="lp-demo">* Số liệu minh họa trong giai đoạn demo, sẽ được cập nhật bằng dữ liệu thực tế.</p>
          </div>
          <div className="lp-mosaic">
            {MOSAIC.map((k) => <div key={k} style={{ backgroundImage: `url(${img(k)})` }} />)}
          </div>
        </div>
      </section>

      {/* ---------- 09 Đánh giá ---------- */}
      <section className="lp-sec container">
        <Reviews />
      </section>

      {/* ---------- 10 Newsletter ---------- */}
      <section className="lp-sec container"><Newsletter /></section>

      {/* ---------- 11 Tin tức ---------- */}
      {tin.length > 0 && (
        <section className="lp-sec container">
          <Head eyebrow="Tạp chí" title="Tin tức & sự kiện" to="/tin-tuc" more="Xem tất cả bài viết" />
          <div className="grid grid-3">
            {tin.map((n) => (
              <Link key={n.tinTucId} to={`/tin-tuc/${n.slug}`} className="lp-news" data-reveal>
                <div className="pic"><span style={{ backgroundImage: `url(${imgUrl(n.anhBia) || anhMacDinh(n)})` }} /></div>
                <div className="body">
                  <small><b>{tenChuyenMuc(n.chuyenMuc)}</b> · {fmtDate(n.ngayDang)}</small>
                  <h3>{n.tieuDe}</h3>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* ---------- 12 FAQ ---------- */}
      <section className="lp-sec container lp-faq-wrap">
        <div className="lp-faq-intro">
          <span className="lp-eyebrow">Hỏi đáp</span>
          <h2>Câu hỏi thường gặp</h2>
          <p>Chưa thấy câu trả lời bạn cần? Hãy để lại câu hỏi, chúng tôi phản hồi trong giờ làm việc.</p>
          <Link className="lp-btn primary sm" to="/lien-he">Gửi câu hỏi</Link>
        </div>
        <div className="lp-faq">
          {FAQ.map(([q, a]) => (
            <details key={q}>
              <summary>{q}</summary>
              <p>{a}</p>
            </details>
          ))}
        </div>
      </section>
    </div>
  )
}

function Head({ eyebrow, title, to, more = 'Xem tất cả' }) {
  return (
    <div className="lp-head">
      <div>{eyebrow && <span className="lp-eyebrow">{eyebrow}</span>}<h2>{title}</h2></div>
      {to && <Link to={to} className="lp-more">{more} →</Link>}
    </div>
  )
}

/** Đánh giá: dải thẻ cuộn ngang (vuốt trên điện thoại, nút mũi tên trên máy tính) */
function Reviews() {
  const track = useRef(null)
  const go = (dir) => {
    const el = track.current
    if (el) el.scrollBy({ left: dir * (el.clientWidth * 0.8), behavior: 'smooth' })
  }
  return (
    <>
      <div className="lp-head">
        <div><span className="lp-eyebrow">Đánh giá</span><h2>Khách hàng nói gì</h2></div>
        <div className="lp-arrows">
          <button type="button" onClick={() => go(-1)} aria-label="Đánh giá trước">←</button>
          <button type="button" onClick={() => go(1)} aria-label="Đánh giá sau">→</button>
        </div>
      </div>
      <div className="lp-reviews" ref={track} tabIndex={0} aria-label="Danh sách đánh giá">
        {DANH_GIA.map((p) => (
          <figure key={p.ten} className="lp-testi">
            <div className="stars" aria-label={`${p.sao} trên 5 sao`}>{'★'.repeat(p.sao)}<span>{'★'.repeat(5 - p.sao)}</span></div>
            <blockquote>“{p.nd}”</blockquote>
            <div className="lp-testi-tour">{p.tour}</div>
            <figcaption>
              <span className="av" aria-hidden="true">{p.ten.split(' ').pop()[0]}</span>
              <div><b>{p.ten}</b><small>{p.noi} · {p.thang}</small></div>
            </figcaption>
          </figure>
        ))}
      </div>
      <p className="lp-demo">* Đánh giá minh họa trong giai đoạn demo.</p>
    </>
  )
}

function Newsletter() {
  const [email, setEmail] = useState('')
  const [st, setSt] = useState({ busy: false, ok: '', err: '' })
  const submit = async (e) => {
    e.preventDefault()
    setSt({ busy: true, ok: '', err: '' })
    try {
      await api.post('/api/public/lien-he', {
        hoTen: 'Khách đăng ký bản tin', email, tieuDe: 'Đăng ký nhận bản tin', noiDung: 'Đăng ký nhận bản tin: tour mới, ưu đãi, lễ hội, kinh nghiệm du lịch',
      })
      setSt({ busy: false, ok: 'Cảm ơn bạn! Bản tin sẽ được gửi đến email của bạn.', err: '' })
      setEmail('')
    } catch (x) {
      setSt({ busy: false, ok: '', err: errMsg(x) })
    }
  }
  return (
    <div className="lp-news-box" data-reveal>
      <div className="lp-orb c" />
      <div>
        <h2>Nhận cảm hứng cho <span className="lp-grad-text">chuyến đi tiếp theo</span></h2>
        <ul className="lp-news-list">
          <li>Tour mới</li><li>Ưu đãi du lịch</li><li>Lễ hội & sự kiện</li><li>Kinh nghiệm khám phá miền Tây</li>
        </ul>
      </div>
      <form onSubmit={submit}>
        <input type="email" required placeholder="Email của bạn" aria-label="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <button className="lp-btn primary" disabled={st.busy}>{st.busy ? 'Đang gửi…' : 'Đăng ký'}</button>
        {(st.ok || st.err) && <div className={`msg ${st.err ? 'err' : ''}`} role="status">{st.ok || st.err}</div>}
      </form>
    </div>
  )
}
