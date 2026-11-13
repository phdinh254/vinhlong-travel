import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { errMsg, money } from '../../api'
import { TourCard, Pagination, Loading, Alert, Modal } from '../../components/ui.jsx'
import { tourImage, tenKhuVuc } from '../../tourUtils.js'
import { SO_NGAY, NGAN_SACH, SAP_XEP, ANH_KHU_VUC } from '../../content/timTour.js'
import { useReveal } from '../../useReveal.js'
import './tours.css'

/**
 * "Tour du lịch": tìm kiếm, so sánh và chọn tour (Q02, Q03, Q07).
 * - Bộ lọc gửi lên API: q, diemDenId, khuVucId, loaiHinhId, soNgay, dichVuId
 * - Xử lý trên trình duyệt: sort, gia (ngân sách), page — toàn bộ kết quả được tải một lần (tối đa 50 tour)
 * Mọi điều kiện giữ trên URL để chia sẻ / quay lại.
 */
const API_KEYS = ['q', 'diemDenId', 'khuVucId', 'loaiHinhId', 'soNgay', 'dichVuId']
const PAGE_SIZE = 9
const MAX_SO_SANH = 3
const ngay = (v) => (v ? new Date(v).toLocaleDateString('vi-VN') : '—')

function FilterSelect({ k, label, options, value, onChange, all = 'Tất cả' }) {
  return (
    <div className="field">
      <label htmlFor={`f-${k}`}>{label}</label>
      <select id={`f-${k}`} value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">{all}</option>
        {options.map((o) => <option key={o.id} value={o.id}>{o.ten}</option>)}
      </select>
    </div>
  )
}

export default function Tours() {
  const [sp, setSp] = useSearchParams()
  const [dm, setDm] = useState({ loaiHinhs: [], khuVucs: [], dichVus: [], diemDens: [] })
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
    api.get('/api/public/danh-muc').then(async (r) => {
      setDm(r.data)
      // Đếm số tour theo khu vực cho ô "Khám phá theo khu vực"
      const res = await Promise.all(r.data.khuVucs.map((k) =>
        api.get('/api/public/tours', { params: { khuVucId: k.id, size: 1 } }).then((x) => [k.id, x.data.totalElements]).catch(() => [k.id, 0])))
      setDemKhuVuc(Object.fromEntries(res))
    }).catch(() => {})
  }, [])

  useEffect(() => {
    let alive = true
    const p = Object.fromEntries(new URLSearchParams(apiQuery))
    api.get('/api/public/tours', { params: { ...p, size: 50 } })
      .then((r) => alive && setData(r.data)).catch((e) => alive && setErr(errMsg(e)))
    return () => { alive = false }
  }, [apiQuery])

  /** Lọc ngân sách + sắp xếp trên toàn bộ kết quả */
  const ketQua = useMemo(() => {
    let list = data?.content || []
    const ns = NGAN_SACH.find((x) => x.id === params.gia)
    if (ns) list = list.filter((t) => t.giaTu != null && t.giaTu >= ns.min && t.giaTu < ns.max)
    const by = {
      'gia-tang': (a, b) => (a.giaTu ?? Infinity) - (b.giaTu ?? Infinity),
      'gia-giam': (a, b) => (b.giaTu ?? -1) - (a.giaTu ?? -1),
      'khoi-hanh': (a, b) => (a.ngayKhoiHanh || '9999').localeCompare(b.ngayKhoiHanh || '9999'),
      'ngan-ngay': (a, b) => (a.soNgay ?? 99) - (b.soNgay ?? 99),
    }[params.sort]
    return by ? [...list].sort(by) : list
  }, [data, params.gia, params.sort])

  // Trang đầu, chưa lọc, sắp xếp mặc định: tour nổi bật đầu tiên hiển thị dạng thẻ lớn
  const noiBat = !coLoc && !params.sort && page === 0 ? ketQua.find((t) => t.noiBat) : null
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
    params.diemDenId && ['diemDenId', dm.diemDens.find((d) => String(d.diemDenId) === params.diemDenId)?.tenDiemDen || 'Điểm đến'],
    params.khuVucId && ['khuVucId', tenKhuVuc(dm.khuVucs.find((d) => String(d.id) === params.khuVucId)?.ten || 'Khu vực')],
    params.loaiHinhId && ['loaiHinhId', dm.loaiHinhs.find((d) => String(d.id) === params.loaiHinhId)?.ten || 'Loại hình'],
    params.soNgay && ['soNgay', SO_NGAY.find((d) => d.id === params.soNgay)?.ten],
    params.dichVuId && ['dichVuId', `Có ${dm.dichVus.find((d) => String(d.id) === params.dichVuId)?.ten?.toLowerCase() || 'dịch vụ'}`],
    params.gia && ['gia', NGAN_SACH.find((d) => d.id === params.gia)?.ten],
  ].filter(Boolean)

  const toggleSoSanh = (t) => setSoSanh((cur) => cur.some((x) => x.tourId === t.tourId)
    ? cur.filter((x) => x.tourId !== t.tourId)
    : cur.length >= MAX_SO_SANH ? cur : [...cur, t])
  const dangSoSanh = (t) => soSanh.some((x) => x.tourId === t.tourId)

  return (
    <div className={`tr ${soSanh.length ? 'has-tray' : ''}`} ref={root}>
      {/* ---------- Hero tìm kiếm ---------- */}
      <header className="tr-hero" style={{ backgroundImage: 'url(/images/song-nuoc.jpg)' }}>
        <div className="container">
          <span className="tr-eyebrow">Tour du lịch</span>
          <h1>Chọn hành trình cho chuyến đi của bạn</h1>
          <p>Tour trọn gói Vĩnh Long – Bến Tre – Trà Vinh: so sánh lịch trình, giá và đặt chỗ trực tuyến.</p>
          <form className="tr-search" onSubmit={(e) => { e.preventDefault(); set('q', q.trim()) }} role="search">
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Tìm theo tên tour, mã tour…" aria-label="Từ khóa" />
            <button className="btn sun">Tìm tour</button>
          </form>
          <div className="tr-stats">
            <span><b>{data ? data.totalElements : '…'}</b> tour phù hợp</span>
            <span><b>3</b> khu vực</span>
            <span>Thanh toán qua <b>VNPay</b></span>
          </div>
        </div>
      </header>

      <div className="container">
        {/* ---------- Khám phá theo khu vực (khi chưa lọc) ---------- */}
        {!coLoc && dm.khuVucs.length > 0 && (
          <section className="tr-regions" aria-label="Khám phá theo khu vực">
            {dm.khuVucs.map((k, i) => {
              const meta = ANH_KHU_VUC.find(([ten]) => k.ten.includes(ten))
              return (
                <button key={k.id} type="button" className="tr-region" onClick={() => set('khuVucId', String(k.id))} data-reveal style={{ '--d': `${i * 80}ms` }}>
                  <span className="bg" style={{ backgroundImage: `url(/images/${meta?.[1] || 'song-nuoc'}.jpg)` }} aria-hidden="true" />
                  <span className="cap">
                    <small>{demKhuVuc[k.id] ?? '…'} tour</small>
                    <b>{tenKhuVuc(k.ten)}</b>
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
            <FilterSelect {...sel('diemDenId')} label="Điểm đến" options={dm.diemDens.map((d) => ({ id: d.diemDenId, ten: d.tenDiemDen }))} />
            <FilterSelect {...sel('khuVucId')} label="Khu vực" options={dm.khuVucs.map((k) => ({ id: k.id, ten: tenKhuVuc(k.ten) }))} />
            <FilterSelect {...sel('loaiHinhId')} label="Loại hình" options={dm.loaiHinhs} />
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
            <FilterSelect {...sel('dichVuId')} label="Có bao gồm dịch vụ" options={dm.dichVus} all="Không lọc" />
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
                    <Link to={`/tour/${noiBat.tourId}`} className="tr-feature-img" style={{ backgroundImage: `url(${tourImage(noiBat)})` }} aria-label={noiBat.tenTour}>
                      <span className="tc-tag hot">Tour nổi bật</span>
                    </Link>
                    <div className="tr-feature-body">
                      <div className="tc-region">{(noiBat.khuVucs || []).map(tenKhuVuc).join(' · ')}</div>
                      <h2><Link to={`/tour/${noiBat.tourId}`}>{noiBat.tenTour}</Link></h2>
                      {noiBat.moTaNgan && <p>{noiBat.moTaNgan}</p>}
                      <ul className="tr-feature-facts">
                        <li><small>Thời lượng</small>{noiBat.thoiLuong}</li>
                        <li><small>Khởi hành gần nhất</small>{ngay(noiBat.ngayKhoiHanh)}</li>
                        <li><small>Điểm đón</small>{noiBat.diemKhoiHanh || 'Liên hệ'}</li>
                      </ul>
                      <div className="tr-feature-foot">
                        <div><small>Giá từ</small><b className="price">{noiBat.giaTu ? money(noiBat.giaTu) : 'Liên hệ'}</b></div>
                        <div className="tc-actions">
                          <button type="button" className="btn secondary sm" aria-pressed={dangSoSanh(noiBat)} onClick={() => toggleSoSanh(noiBat)}>
                            {dangSoSanh(noiBat) ? '✓ Đang so sánh' : '+ So sánh'}
                          </button>
                          <Link className="btn sm" to={`/tour/${noiBat.tourId}#lich-khoi-hanh`}>Đặt tour</Link>
                        </div>
                      </div>
                    </div>
                  </article>
                )}
                <div className="tr-grid">
                  {trang.map((t, i) => (
                    <div key={t.tourId} data-reveal style={{ '--d': `${(i % 3) * 70}ms` }}>
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
                <span key={t.tourId} className="tr-tray-item">
                  <img src={tourImage(t)} alt="" />
                  <b>{t.tenTour}</b>
                  <button type="button" onClick={() => toggleSoSanh(t)} aria-label={`Bỏ ${t.tenTour} khỏi so sánh`}>✕</button>
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
                  {soSanh.map((t) => <th key={t.tourId} scope="col"><img src={tourImage(t)} alt="" />{t.tenTour}</th>)}</tr>
              </thead>
              <tbody>
                {[
                  ['Giá từ', (t) => <b className="price">{t.giaTu ? money(t.giaTu) : 'Liên hệ'}</b>],
                  ['Thời lượng', (t) => t.thoiLuong],
                  ['Khu vực', (t) => (t.khuVucs || []).map(tenKhuVuc).join(', ')],
                  ['Loại hình', (t) => (t.loaiHinhs || []).join(', ')],
                  ['Khởi hành gần nhất', (t) => ngay(t.ngayKhoiHanh)],
                  ['Điểm đón', (t) => t.diemKhoiHanh || 'Liên hệ'],
                  ['Chỗ còn (đợt gần nhất)', (t) => t.choConLai ?? '—'],
                ].map(([label, get]) => (
                  <tr key={label}><th scope="row">{label}</th>{soSanh.map((t) => <td key={t.tourId}>{get(t)}</td>)}</tr>
                ))}
                <tr><th scope="row" />{soSanh.map((t) => (
                  <td key={t.tourId}><Link className="btn sm" to={`/tour/${t.tourId}#lich-khoi-hanh`}>Đặt tour</Link></td>
                ))}</tr>
              </tbody>
            </table>
          </div>
        </Modal>
      )}
    </div>
  )
}
