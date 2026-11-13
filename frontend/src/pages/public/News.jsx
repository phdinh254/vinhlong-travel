import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { fmtDate, imgUrl } from '../../api'
import { Pagination, Loading } from '../../components/ui.jsx'
import { CHUYEN_MUC, tenChuyenMuc, anhMacDinh, phutDoc } from '../../content/chuyenMuc.js'
import './news.css'

const PAGE_SIZE = 12
const NOW = new Date()
const SO_BAO = `Số tháng ${NOW.getMonth() + 1}/${NOW.getFullYear()}`

/** Thứ tự ô trong lưới bento (4 cột): lớn 2×2, cao 1×2, ảnh, ghi chú, rộng 2×1, ảnh, ghi chú */
const BENTO = ['feature', 'tall', 'photo', 'note', 'wide', 'photo', 'note leaf']

const cover = (n) => imgUrl(n.anhBia) || anhMacDinh(n)

function Meta({ n, light }) {
  return (
    <div className={`nw-meta ${light ? 'light' : ''}`}>
      <span className="nw-cat">{tenChuyenMuc(n.chuyenMuc)}</span>
      <span>{fmtDate(n.ngayDang)}</span>
      <span>{phutDoc(n.noiDung)} phút đọc</span>
    </div>
  )
}

/** Một ô trong lưới bento */
function BentoItem({ n, kind }) {
  const isNote = kind.startsWith('note')
  return (
    <Link to={`/tin-tuc/${n.slug}`} className={`nw-b ${kind}`}
      style={isNote ? undefined : { backgroundImage: `url(${cover(n)})` }}>
      <div className="nw-b-body">
        <Meta n={n} light={!isNote} />
        <h3>{n.tieuDe}</h3>
        {(kind === 'feature' || isNote) && n.tomTat && <p>{n.tomTat}</p>}
        {isNote && <span className="nw-more">Đọc tiếp →</span>}
      </div>
    </Link>
  )
}

/** Một dòng trong danh sách "Mới cập nhật" */
function Row({ n }) {
  return (
    <article className="nw-row">
      <Link to={`/tin-tuc/${n.slug}`} className="nw-row-img" style={{ backgroundImage: `url(${cover(n)})` }} aria-hidden="true" tabIndex={-1} />
      <div>
        <Meta n={n} />
        <h3><Link to={`/tin-tuc/${n.slug}`}>{n.tieuDe}</Link></h3>
        {n.tomTat && <p>{n.tomTat}</p>}
      </div>
    </article>
  )
}

export default function News() {
  const [params, setParams] = useSearchParams()
  const chuyenMuc = params.get('chuyenMuc') || ''
  const page = Number(params.get('trang') || 0)
  const [data, setData] = useState(null)

  useEffect(() => {
    let alive = true
    api.get('/api/public/tin-tuc', { params: { page, size: PAGE_SIZE, chuyenMuc: chuyenMuc || undefined } })
      .then((r) => alive && setData(r.data))
      .catch(() => alive && setData({ content: [], totalPages: 0 }))
    return () => { alive = false }
  }, [page, chuyenMuc])

  const go = (next) => {
    const p = new URLSearchParams()
    const cm = next.chuyenMuc ?? chuyenMuc
    if (cm) p.set('chuyenMuc', cm)
    if (next.trang) p.set('trang', next.trang)
    setParams(p)
    setData(null)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const items = data?.content || []
  const showBento = page === 0
  const bento = showBento ? items.slice(0, BENTO.length) : []
  const rest = showBento ? items.slice(BENTO.length) : items
  const popular = [...items].sort((a, b) => b.luotXem - a.luotXem).slice(0, 5)
  const current = CHUYEN_MUC.find((c) => c.key === chuyenMuc)

  return (
    <div className="nw">
      {/* ---------- Masthead ---------- */}
      <header className="nw-mast">
        <svg className="nw-leaf" viewBox="0 0 200 200" aria-hidden="true">
          <path d="M100 10C150 40 190 90 170 140S90 200 50 170 10 80 40 50 70 0 100 10Z" />
        </svg>
        <div className="container">
          <div className="nw-issue"><span>Tạp chí du lịch Vĩnh Long</span><span>{SO_BAO}</span></div>
          <h1>Chuyện miền sông nước</h1>
          <p className="nw-lead">{current ? current.desc : 'Tin tức, câu chuyện văn hóa, sự kiện và kinh nghiệm du lịch từ Vĩnh Long, Bến Tre và Trà Vinh.'}</p>
          <nav className="nw-tabs" aria-label="Chuyên mục">
            <button className={!chuyenMuc ? 'active' : ''} onClick={() => go({ chuyenMuc: '', trang: 0 })}>Tất cả</button>
            {CHUYEN_MUC.map((c) => (
              <button key={c.key} className={chuyenMuc === c.key ? 'active' : ''} onClick={() => go({ chuyenMuc: c.key, trang: 0 })}>{c.label}</button>
            ))}
          </nav>
        </div>
      </header>

      <div className="container">
        {!data ? <Loading /> : items.length === 0 ? (
          <p className="nw-empty">Chưa có bài viết trong chuyên mục này.</p>
        ) : (
          <>
            {/* ---------- Bento ---------- */}
            {bento.length > 0 && (
              <section className="nw-bento" aria-label="Bài nổi bật">
                {bento.map((n, i) => <BentoItem key={n.tinTucId} n={n} kind={BENTO[i]} />)}
              </section>
            )}

            {/* ---------- Danh sách + Đọc nhiều ---------- */}
            {(rest.length > 0 || popular.length > 0) && (
              <div className="nw-columns">
                <section>
                  {rest.length > 0 && <>
                    <h2 className="nw-h2">Mới cập nhật</h2>
                    <div className="nw-list">{rest.map((n) => <Row key={n.tinTucId} n={n} />)}</div>
                  </>}
                  <Pagination page={page} totalPages={data.totalPages} onChange={(p) => go({ trang: p })} />
                </section>
                <aside className="nw-side">
                  <h2 className="nw-h2">Đọc nhiều</h2>
                  <ol className="nw-popular">
                    {popular.map((n) => (
                      <li key={n.tinTucId}>
                        <Link to={`/tin-tuc/${n.slug}`}>{n.tieuDe}</Link>
                        <span>{tenChuyenMuc(n.chuyenMuc)} · {n.luotXem} lượt xem</span>
                      </li>
                    ))}
                  </ol>
                  <div className="nw-promo">
                    <h3>Đọc xong rồi đi thôi!</h3>
                    <p>Biến cảm hứng thành chuyến đi với các tour trọn gói khắp Vĩnh Long mới.</p>
                    <Link className="btn sun" to="/tour">Xem tour</Link>
                  </div>
                </aside>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
