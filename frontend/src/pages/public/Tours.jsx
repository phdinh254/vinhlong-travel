import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { errMsg, money, loadCatalogs, EMPTY_CATALOGS, soNgayParams } from '../../api'
import { TourCard, Pagination, Loading, Alert, Modal } from '../../components/ui.jsx'
import { tourImage, tenKhuVuc, thoiLuong } from '../../tourUtils.js'
import { SO_NGAY, NGAN_SACH, SAP_XEP, ANH_KHU_VUC } from '../../content/timTour.js'
import { useReveal } from '../../useReveal.js'
import './tours.css'

/**
 * "Tour du lịch": tìm kiếm, so sánh và chọn tour (Q02, Q03, Q07).
 * - Bộ lọc gửi lên API: q, destinationId, regionId, categoryId, amenityId, soNgay (-> minDays/maxDays)
 * - Xử lý trên trình duyệt: sort, gia (ngân sách), page — toàn bộ kết quả được tải một lần (tối đa 50 tour)
 * Mọi điều kiện giữ trên URL để chia sẻ / quay lại.
 */
const API_KEYS = ['q', 'destinationId', 'regionId', 'categoryId', 'soNgay', 'amenityId']
const PAGE_SIZE = 9
const MAX_SO_SANH = 3
const ngay = (v) => (v ? new Date(v).toLocaleDateString('vi-VN') : '—')

function FilterSelect({ k, label, options, value, onChange, all = 'Tất cả' }) {
  return (
    <div className="field">
      <label htmlFor={`f-${k}`}>{label}</label>
      <select id={`f-${k}`} value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">{all}</option>
        {options.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
      </select>
    </div>
  )
}

export default function Tours() {
  const [sp, setSp] = useSearchParams()
  const [dm, setDm] = useState(EMPTY_CATALOGS)
  const [data, setData] = useState(null)
  const [err, setErr] = useState('')
  const [q, setQ] = useState(sp.get('q') || '')
  const [demKhuVuc, setDemKhuVuc] = useState({})
  const [moLoc, setMoLoc] = useState(false)
  const [soSanh, setSoSanh] = useState([])
  const [xemSoSanh, setXemSoSanh] = useState(false)
  const root = useReveal()

  const params = Object.fromEntries(sp.entries())
  const page = Number(sp.get('page') || 0)
  const apiQuery = API_KEYS.filter((k) => params[k]).map((k) => `${k}=${encodeURIComponent(params[k])}`).join('&')
  const coLoc = API_KEYS.some((k) => params[k]) || Boolean(params.gia)

  useEffect(() => {
    loadCatalogs().then(async (c) => {
      setDm(c)
      // Đếm số tour theo khu vực cho ô "Khám phá theo khu vực"
      const res = await Promise.all(c.regions.map((k) =>
        api.get('/api/v1/tours', { params: { regionId: k.id, size: 1 } }).then((x) => [k.id, x.data.total]).catch(() => [k.id, 0])))
      setDemKhuVuc(Object.fromEntries(res))
    })
  }, [])

  useEffect(() => {
    let alive = true
    const { soNgay, ...p } = Object.fromEntries(new URLSearchParams(apiQuery))
    api.get('/api/v1/tours', { params: { ...p, ...soNgayParams(soNgay), size: 50 } })
      .then((r) => alive && setData(r.data)).catch((e) => alive && setErr(errMsg(e)))
    return () => { alive = false }
  }, [apiQuery])

  /** Lọc ngân sách + sắp xếp trên toàn bộ kết quả */
  const ketQua = useMemo(() => {
    let list = data?.items || []
    const ns = NGAN_SACH.find((x) => x.id === params.gia)
    if (ns) list = list.filter((t) => t.fromPrice != null && t.fromPrice >= ns.min && t.fromPrice < ns.max)
    const by = {
      'gia-tang': (a, b) => (a.fromPrice ?? Infinity) - (b.fromPrice ?? Infinity),
      'gia-giam': (a, b) => (b.fromPrice ?? -1) - (a.fromPrice ?? -1),
      'khoi-hanh': (a, b) => (a.nextDepartureAt || '9999').localeCompare(b.nextDepartureAt || '9999'),
      'ngan-ngay': (a, b) => (a.durationDays ?? 99) - (b.durationDays ?? 99),
    }[params.sort]
    return by ? [...list].sort(by) : list
  }, [data, params.gia, params.sort])

  // Trang đầu, chưa lọc, sắp xếp mặc định: tour nổi bật đầu tiên hiển thị dạng thẻ lớn
  const noiBat = !coLoc && !params.sort && page === 0 ? ketQua.find((t) => t.featured) : null
  const conLai = noiBat ? ketQua.filter((t) => t !== noiBat) : ketQua
  const totalPages = Math.ceil(conLai.length / PAGE_SIZE)
  const trang = conLai.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const set = (k, v) => {
    const n = { ...params, [k]: v }
    if (!v) delete n[k]
    if (k !== 'page') delete n.page
    if (API_KEYS.includes(k)) setData(null)
    setErr('')
    setSp(n)
  }
  const clear = () => { setQ(''); setData(null); setSp(params.sort ? { sort: params.sort } : {}) }
  const sel = (k) => ({ k, value: params[k] || '', onChange: (v) => set(k, v) })

  /** Nhãn của các bộ lọc đang bật (hiện dạng chip, bấm để bỏ) */
  const chips = [
    params.q && ['q', `“${params.q}”`],
    params.destinationId && ['destinationId', dm.destinations.find((d) => String(d.id) === params.destinationId)?.name || 'Điểm đến'],
    params.regionId && ['regionId', tenKhuVuc(dm.regions.find((d) => String(d.id) === params.regionId)?.name || 'Khu vực')],
    params.categoryId && ['categoryId', dm.categories.find((d) => String(d.id) === params.categoryId)?.name || 'Loại hình'],
    params.soNgay && ['soNgay', SO_NGAY.find((d) => d.id === params.soNgay)?.ten],
    params.amenityId && ['amenityId', `Có ${dm.amenities.find((d) => String(d.id) === params.amenityId)?.name?.toLowerCase() || 'dịch vụ'}`],
    params.gia && ['gia', NGAN_SACH.find((d) => d.id === params.gia)?.ten],
  ].filter(Boolean)

  const toggleSoSanh = (t) => setSoSanh((cur) => cur.some((x) => x.id === t.id)
    ? cur.filter((x) => x.id !== t.id)
    : cur.length >= MAX_SO_SANH ? cur : [...cur, t])
  const dangSoSanh = (t) => soSanh.some((x) => x.id === t.id)

  return (
    <div className={`tr ${soSanh.length ? 'has-tray' : ''}`} ref={root}>
      {/* ---------- Hero tìm kiếm ---------- */}
      <header className="tr-hero" style={{ backgroundImage: 'url(/images/song-nuoc.jpg)' }}>
        <div className="container">
          <span className="eyebrow light">Tour du lịch</span>
          <h1>Chọn hành trình cho chuyến đi của bạn</h1>
          <p>Tour trọn gói Vĩnh Long – Bến Tre – Trà Vinh: so sánh lịch trình, giá và đặt chỗ trực tuyến.</p>
          <form className="tr-search" onSubmit={(e) => { e.preventDefault(); set('q', q.trim()) }} role="search">
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Tìm theo tên tour, mã tour…" aria-label="Từ khóa" />
            <button className="btn sun">Tìm tour</button>
          </form>
          <div className="tr-stats">
            <span><b>{data ? data.total : '…'}</b> tour phù hợp</span>
            <span><b>3</b> khu vực</span>
            <span>Thanh toán qua <b>VNPay</b></span>
          </div>
        </div>
      </header>

      <div className="container">
        {/* ---------- Khám phá theo khu vực (khi chưa lọc) ---------- */}
        {!coLoc && dm.regions.length > 0 && (
          <section className="tr-regions" aria-label="Khám phá theo khu vực">
            {dm.regions.map((k, i) => {
              const meta = ANH_KHU_VUC.find(([ten]) => k.name.includes(ten))
              return (
                <button key={k.id} type="button" className="tr-region" onClick={() => set('regionId', String(k.id))} data-reveal style={{ '--d': `${i * 80}ms` }}>
                  <span className="bg" style={{ backgroundImage: `url(/images/${meta?.[1] || 'song-nuoc'}.jpg)` }} aria-hidden="true" />
                  <span className="cap">
                    <small>{demKhuVuc[k.id] ?? '…'} tour</small>
                    <b>{tenKhuVuc(k.name)}</b>
                    {meta && <em>{meta[2]}</em>}
                  </span>
                </button>
              )
            })}
          </section>
        )}

        <div className="tr-main">
          {/* ---------- Bộ lọc ---------- */}
          <aside className={`tr-filter ${moLoc ? 'open' : ''}`} aria-label="Bộ lọc tour">
            <div className="tr-filter-head">
              <h2>Bộ lọc</h2>
              <button type="button" className="tr-close" onClick={() => setMoLoc(false)} aria-label="Đóng bộ lọc">✕</button>
            </div>
            <FilterSelect {...sel('destinationId')} label="Điểm đến" options={dm.destinations} />
            <FilterSelect {...sel('regionId')} label="Khu vực" options={dm.regions.map((k) => ({ id: k.id, name: tenKhuVuc(k.name) }))} />
            <FilterSelect {...sel('categoryId')} label="Loại hình" options={dm.categories} />
            <div className="field">
              <span className="tr-label">Thời lượng</span>
              <div className="tr-pills">
                {SO_NGAY.map((s) => (
                  <button key={s.id} type="button" className={params.soNgay === s.id ? 'on' : ''} aria-pressed={params.soNgay === s.id}
                    onClick={() => set('soNgay', params.soNgay === s.id ? '' : s.id)}>{s.ten}</button>
                ))}
              </div>
            </div>
            <div className="field">
              <span className="tr-label">Ngân sách / khách</span>
              <div className="tr-pills col">
                {NGAN_SACH.map((s) => (
                  <button key={s.id} type="button" className={params.gia === s.id ? 'on' : ''} aria-pressed={params.gia === s.id}
                    onClick={() => set('gia', params.gia === s.id ? '' : s.id)}>{s.ten}</button>
                ))}
              </div>
            </div>
            <FilterSelect {...sel('amenityId')} label="Có bao gồm dịch vụ" options={dm.amenities} all="Không lọc" />
            {chips.length > 0 && <button type="button" className="btn secondary sm" onClick={clear}>Xóa tất cả bộ lọc</button>}
            <button type="button" className="btn sm tr-apply" onClick={() => setMoLoc(false)}>Xem {ketQua.length} tour</button>
          </aside>
          {moLoc && <div className="tr-backdrop" onClick={() => setMoLoc(false)} aria-hidden="true" />}

          {/* ---------- Kết quả ---------- */}
          <section className="tr-results" aria-live="polite">
            <div className="tr-toolbar">
              <div className="tr-count">
                <button type="button" className="btn secondary sm tr-open" onClick={() => setMoLoc(true)}>
                  Bộ lọc{chips.length ? ` (${chips.length})` : ''}
                </button>
                {data && <span><b>{ketQua.length}</b> tour</span>}
              </div>
              <label className="tr-sort">Sắp xếp
                <select value={params.sort || ''} onChange={(e) => set('sort', e.target.value)}>
                  {SAP_XEP.map((s) => <option key={s.id} value={s.id}>{s.ten}</option>)}
                </select>
              </label>
            </div>
            {chips.length > 0 && (
              <div className="tr-chips">
                {chips.map(([k, label]) => (
                  <button key={k} type="button" onClick={() => { if (k === 'q') setQ(''); set(k, '') }} aria-label={`Bỏ lọc ${label}`}>
                    {label} <span aria-hidden="true">✕</span>
                  </button>
                ))}
              </div>
            )}

            <Alert>{err}</Alert>
            {!data ? <Loading /> : ketQua.length === 0 ? (
              <div className="tr-empty">
                <h3>Chưa có tour phù hợp</h3>
                <p>Thử bỏ bớt bộ lọc, hoặc để chúng tôi gợi ý hành trình theo nhu cầu của bạn.</p>
                <div className="row" style={{ justifyContent: 'center' }}>
                  <button className="btn secondary sm" onClick={clear}>Xem tất cả tour</button>
                  <Link className="btn sm" to="/lien-he?loai=tu-van">Nhờ tư vấn</Link>
                </div>
              </div>
            ) : (
              <>
                {noiBat && (
                  <article className="tr-feature" data-reveal>
                    <Link to={`/tour/${noiBat.id}`} className="tr-feature-img" style={{ backgroundImage: `url(${tourImage(noiBat)})` }} aria-label={noiBat.name}>
                      <span className="tc-tag hot">Tour nổi bật</span>
                    </Link>
                    <div className="tr-feature-body">
                      <div className="tc-region">{(noiBat.regions || []).map((r) => tenKhuVuc(r.name)).join(' · ')}</div>
                      <h2><Link to={`/tour/${noiBat.id}`}>{noiBat.name}</Link></h2>
                      {noiBat.summary && <p>{noiBat.summary}</p>}
                      <ul className="tr-feature-facts">
                        <li><small>Thời lượng</small>{thoiLuong(noiBat)}</li>
                        <li><small>Khởi hành gần nhất</small>{ngay(noiBat.nextDepartureAt)}</li>
                        <li><small>Loại hình</small>{(noiBat.categories || []).map((c) => c.name).join(', ') || '—'}</li>
                      </ul>
                      <div className="tr-feature-foot">
                        <div><small>Giá từ</small><b className="price">{noiBat.fromPrice ? money(noiBat.fromPrice) : 'Liên hệ'}</b></div>
                        <div className="tc-actions">
                          <button type="button" className="btn secondary sm" aria-pressed={dangSoSanh(noiBat)} onClick={() => toggleSoSanh(noiBat)}>
                            {dangSoSanh(noiBat) ? '✓ Đang so sánh' : '+ So sánh'}
                          </button>
                          <Link className="btn sm" to={`/tour/${noiBat.id}#lich-khoi-hanh`}>Đặt tour</Link>
                        </div>
                      </div>
                    </div>
                  </article>
                )}
                <div className="tr-grid">
                  {trang.map((t, i) => (
                    <div key={t.id} data-reveal style={{ '--d': `${(i % 3) * 70}ms` }}>
                      <TourCard t={t} onCompare={toggleSoSanh} compared={dangSoSanh(t)} />
                    </div>
                  ))}
                </div>
                {totalPages > 1 && (
                  <Pagination page={page} totalPages={totalPages}
                    onChange={(p) => { set('page', p ? String(p) : ''); window.scrollTo({ top: 0, behavior: 'smooth' }) }} />
                )}
              </>
            )}

            <div className="tr-custom" data-reveal>
              <div>
                <h2>Chưa tìm thấy tour ưng ý?</h2>
                <p>Cho chúng tôi biết số khách, ngày đi và sở thích, chúng tôi sẽ điều chỉnh lịch trình riêng cho nhóm của bạn.</p>
              </div>
              <Link className="btn sun" to="/lien-he?loai=nhom">Yêu cầu tour riêng</Link>
            </div>
          </section>
        </div>
      </div>

      {/* ---------- Khay so sánh ---------- */}
      {soSanh.length > 0 && (
        <div className="tr-tray" role="region" aria-label="So sánh tour">
          <div className="container tr-tray-in">
            <div className="tr-tray-items">
              {soSanh.map((t) => (
                <span key={t.id} className="tr-tray-item">
                  <img src={tourImage(t)} alt="" />
                  <b>{t.name}</b>
                  <button type="button" onClick={() => toggleSoSanh(t)} aria-label={`Bỏ ${t.name} khỏi so sánh`}>✕</button>
                </span>
              ))}
              {soSanh.length < MAX_SO_SANH && <span className="tr-tray-hint">Chọn tối đa {MAX_SO_SANH} tour để so sánh</span>}
            </div>
            <div className="tr-tray-actions">
              <button type="button" className="btn link" onClick={() => setSoSanh([])}>Xóa</button>
              <button type="button" className="btn sun" disabled={soSanh.length < 2} onClick={() => setXemSoSanh(true)}>So sánh ({soSanh.length})</button>
            </div>
          </div>
        </div>
      )}

      {xemSoSanh && (
        <Modal title="So sánh tour" onClose={() => setXemSoSanh(false)}>
          <div className="table-wrap tr-compare">
            <table>
              <thead>
                <tr><th scope="col">Tiêu chí</th>
                  {soSanh.map((t) => <th key={t.id} scope="col"><img src={tourImage(t)} alt="" />{t.name}</th>)}</tr>
              </thead>
              <tbody>
                {[
                  ['Giá từ', (t) => <b className="price">{t.fromPrice ? money(t.fromPrice) : 'Liên hệ'}</b>],
                  ['Thời lượng', (t) => thoiLuong(t)],
                  ['Khu vực', (t) => (t.regions || []).map((r) => tenKhuVuc(r.name)).join(', ')],
                  ['Loại hình', (t) => (t.categories || []).map((c) => c.name).join(', ')],
                  ['Khởi hành gần nhất', (t) => ngay(t.nextDepartureAt)],
                ].map(([label, get]) => (
                  <tr key={label}><th scope="row">{label}</th>{soSanh.map((t) => <td key={t.id}>{get(t)}</td>)}</tr>
                ))}
                <tr><th scope="row" />{soSanh.map((t) => (
                  <td key={t.id}><Link className="btn sm" to={`/tour/${t.id}#lich-khoi-hanh`}>Đặt tour</Link></td>
                ))}</tr>
              </tbody>
            </table>
          </div>
        </Modal>
      )}
    </div>
  )
}
