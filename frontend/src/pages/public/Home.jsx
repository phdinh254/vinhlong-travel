import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { errMsg, fmtDate, imgUrl, money, loadCatalogs, EMPTY_CATALOGS } from '../../api'
import { TourCard, Loading } from '../../components/ui.jsx'
import { tenChuyenMuc, anhMacDinh } from '../../content/chuyenMuc.js'
import { SO_NGAY } from '../../content/timTour.js'
import { tenKhuVuc } from '../../tourUtils.js'
import SearchSelect, { Icon } from '../../components/SearchSelect.jsx'
import { DIEM_DEN, TRAI_NGHIEM, QUY_TRINH, SO_LIEU, DANH_GIA, FAQ } from '../../content/trangChu.js'
import { useReveal } from '../../useReveal.js'
import './home.css'

const img = (k) => `/images/${k}.jpg`
/** Ảnh nhân vật hero – thay bằng ảnh PNG tách nền (vd. /images/hdv.png) để nhân vật nổi trên khối tròn */
const HERO_NHAN_VAT = img('cho-noi')
/** Ảnh gallery trải nghiệm: [tên ảnh, hình dạng (sq vuông · land ngang · wide rộng · tall đứng), mô tả] — 3 hàng chạy ngang */
const GALLERY = [
  [['vui-choi', 'land', 'Vui chơi ở ao Bà Om'], ['keo-dua', 'sq', 'Kẹo dừa Bến Tre'], ['cho-noi', 'tall', 'Chợ nổi Cái Bè'],
    ['an-binh', 'land', 'Cù lao An Bình'], ['chua-ang', 'sq', 'Cổng chùa Âng'], ['kenh-rach', 'wide', 'Kênh rạch miền Tây'], ['lang-gom', 'tall', 'Làng gốm Mang Thít']],
  [['song-nuoc', 'wide', 'Đời sống trên sông Mekong'], ['homestay', 'sq', 'Homestay nhà vườn'], ['con-phung', 'land', 'Tàu du lịch cập bến cồn Phụng'],
    ['van-thanh-mieu', 'tall', 'Văn Thánh Miếu Vĩnh Long'], ['kenh-rach-2', 'land', 'Xuồng trên kênh rạch'], ['chua-hang', 'sq', 'Chính điện chùa Hang']],
  [['mang-thit', 'sq', 'Lò gạch Mang Thít'], ['ao-ba-om', 'land', 'Ao Bà Om'], ['keo-dua', 'tall', 'Làm kẹo dừa'],
    ['cho-noi', 'land', 'Ghe hàng chợ nổi'], ['homestay', 'wide', 'Không gian homestay'], ['vui-choi', 'sq', 'Trò chơi dân gian'], ['an-binh', 'tall', 'Đường làng An Bình']],
]
const ngayNgan = (v) => new Date(v).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })

/** Tạo link /tour?... từ tên điểm đến / loại hình (so khớp với danh mục trong DB), dự phòng bằng từ khóa */
function tourLink(dm, { diemDen, loaiHinh }) {
  const has = (a, b) => a.toLowerCase().includes(b.toLowerCase())
  if (diemDen) {
    const d = dm.destinations.find((x) => has(x.name, diemDen))
    return d ? `/tour?destinationId=${d.id}` : `/tour?q=${encodeURIComponent(diemDen)}`
  }
  const l = dm.categories.find((x) => has(x.name, loaiHinh))
  return l ? `/tour?categoryId=${l.id}` : '/tour'
}

export default function Home() {
  const [tours, setTours] = useState(null)
  const [tongTour, setTongTour] = useState(0)
  const [tin, setTin] = useState([])
  const [dm, setDm] = useState(EMPTY_CATALOGS)
  const [f, setF] = useState({ destinationId: '', regionId: '', categoryId: '', soNgay: '' })
  const nav = useNavigate()
  const root = useReveal()

  useEffect(() => {
    // Tour phổ biến: ưu tiên tour nổi bật, bổ sung tour khác cho đủ 3
    Promise.all([
      api.get('/api/v1/tours', { params: { featured: true, size: 3 } }),
      api.get('/api/v1/tours', { params: { size: 6 } }),
    ]).then(([a, b]) => {
      const list = [...a.data.items]
      for (const t of b.data.items) if (list.length < 3 && !list.some((x) => x.id === t.id)) list.push(t)
      setTours(list)
      setTongTour(b.data.total)
    }).catch(() => setTours([]))
    api.get('/api/v1/articles', { params: { size: 3 } }).then((r) => setTin(r.data.items)).catch(() => {})
    loadCatalogs().then(setDm)
  }, [])

  const search = (e) => {
    e.preventDefault()
    const p = new URLSearchParams(Object.entries(f).filter(([, v]) => v))
    nav(`/tour${p.size ? `?${p}` : ''}`)
  }

  const hero = tours?.[0]
  const sapKhoiHanh = tours?.filter((t) => t.nextDepartureAt).sort((a, b) => a.nextDepartureAt.localeCompare(b.nextDepartureAt))[0]

  return (
    <div className="lp" ref={root}>
      {/* ---------- 02 Hero (Split Hero – phong cách doanh nghiệp hiện đại) ---------- */}
      <section className="lp-hero">
        <div className="lp-hero-dots" aria-hidden="true" />
        <div className="container lp-hero-grid">
          <div className="lp-hero-text">
            <h1 className="lp-in" style={{ '--d': '120ms' }}>Khám phá <span className="lp-hl">Vĩnh Long</span> theo cách của bạn</h1>
            <p className="lp-in" style={{ '--d': '240ms' }}>Tour trọn gói miền sông nước với hướng dẫn viên bản địa. Chọn lịch, đặt chỗ và thanh toán trực tuyến chỉ trong vài phút.</p>
            <div className="lp-cta lp-in" style={{ '--d': '360ms' }}>
              <Link to="/tour" className="lp-btn primary lp-cta-main">Đặt tour ngay →</Link>
            </div>
          </div>

          <div className="lp-visual">
            <div className="lp-blob lp-in" style={{ '--d': '160ms' }} aria-hidden="true" />
            <div className="lp-ring" aria-hidden="true" />
            <div className="lp-figure lp-in" style={{ '--d': '260ms', backgroundImage: `url(${HERO_NHAN_VAT})` }} role="img"
              aria-label="Người dân chèo xuồng ở chợ nổi miền Tây" />
            {hero && (
              <Link to={`/tour/${hero.id}`} className="lp-float f1 lp-in" style={{ '--d': '520ms' }}>
                <small>{hero.featured ? 'Tour nổi bật' : 'Tour gợi ý'}</small>
                <b>{hero.name}</b>
                <span className="lp-float-price">{hero.fromPrice ? `Từ ${money(hero.fromPrice)}` : 'Liên hệ'}</span>
              </Link>
            )}
            {sapKhoiHanh && (
              <Link to={`/tour/${sapKhoiHanh.id}#lich-khoi-hanh`} className="lp-float f2 lp-in" style={{ '--d': '660ms' }}>
                <span className="ic" aria-hidden="true">📅</span>
                <div><b>Khởi hành {ngayNgan(sapKhoiHanh.nextDepartureAt)}</b>
                  <small>Đang nhận khách</small></div>
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
          <SearchSelect label="Điểm đến" icon="pin" value={f.destinationId} onChange={(v) => setF({ ...f, destinationId: v })}
            options={[{ value: '', label: 'Bạn muốn đi đâu?' }, ...dm.destinations.map((d) => ({ value: d.id, label: d.name }))]} />
          <SearchSelect label="Khu vực" icon="map" value={f.regionId} onChange={(v) => setF({ ...f, regionId: v })}
            options={[{ value: '', label: 'Mọi khu vực' }, ...dm.regions.map((k) => ({ value: k.id, label: tenKhuVuc(k.name) }))]} />
          <SearchSelect label="Loại hình" icon="compass" value={f.categoryId} onChange={(v) => setF({ ...f, categoryId: v })}
            options={[{ value: '', label: 'Mọi loại hình' }, ...dm.categories.map((k) => ({ value: k.id, label: k.name }))]} />
          <SearchSelect label="Thời lượng" icon="clock" value={f.soNgay} onChange={(v) => setF({ ...f, soNgay: v })}
            options={[{ value: '', label: 'Bao lâu cũng được' }, ...SO_NGAY.map((s) => ({ value: s.id, label: s.ten }))]} />
          <button className="lp-search-btn">
            <Icon name="search" size={20} /><span>Tìm kiếm tour</span><Icon name="arrow" size={18} className="go" />
          </button>
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
          <div className="grid grid-3 lp-tours">{tours.map((t) => <div key={t.id} data-reveal><TourCard t={t} /></div>)}</div>
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
            <h2>Hành trình được tạo nên từ những trải nghiệm đáng nhớ</h2>
            <div className="lp-stats">
              {SO_LIEU.map(([n, l]) => <div key={l}><b>{n}</b><span>{l}</span></div>)}
            </div>
            <p className="lp-demo">* Số liệu minh họa trong giai đoạn demo, sẽ được cập nhật bằng dữ liệu thực tế.</p>
          </div>
          {/* Gallery: 3 hàng ảnh chạy ngang vô hạn (phải → trái), mỗi hàng nhân đôi danh sách để vòng lặp liền mạch */}
          <div className="lp-gallery" role="group" aria-label="Hình ảnh trải nghiệm du lịch">
            {GALLERY.map((row, i) => (
              <div key={i} className={`lp-marquee r${i + 1}`}>
                <div className="lp-track">
                  {[...row, ...row].map(([k, shape, alt], j) => (
                    <figure key={j} className={`lp-shot ${shape}`} aria-hidden={j >= row.length || undefined}>
                      <img src={img(k)} alt={j < row.length ? alt : ''} loading="lazy" decoding="async" />
                    </figure>
                  ))}
                </div>
              </div>
            ))}
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
              <Link key={n.id} to={`/tin-tuc/${n.slug}`} className="lp-news" data-reveal>
                <div className="pic"><span style={{ backgroundImage: `url(${imgUrl(n.coverImageUrl) || anhMacDinh(n)})` }} /></div>
                <div className="body">
                  <small><b>{tenChuyenMuc(n.category)}</b> · {fmtDate(n.publishedAt)}</small>
                  <h3>{n.title}</h3>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* ---------- 12 FAQ ---------- */}
      <section className="lp-sec container lp-faq-wrap">
        <div className="lp-faq-intro">
          <span className="eyebrow">Hỏi đáp</span>
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
    <div className={`lp-head${to ? '' : ' is-center'}`}>
      <div>{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h2>{title}</h2></div>
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
        <div><span className="eyebrow">Đánh giá</span><h2>Khách hàng nói gì</h2></div>
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
      await api.post('/api/v1/contact-messages', {
        fullName: 'Khách đăng ký bản tin', email, subject: 'Đăng ký nhận bản tin', message: 'Đăng ký nhận bản tin: tour mới, ưu đãi, lễ hội, kinh nghiệm du lịch',
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
