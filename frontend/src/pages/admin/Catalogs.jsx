import { useEffect, useState, useCallback } from 'react'
import api, { errMsg, money, fmtDate, hhmm, pageCount, CACH_TINH_PHU_THU } from '../../api'
import { Alert, Loading, Modal, Pagination } from '../../components/ui.jsx'
import { tenMocPhat } from '../../tourUtils.js'

/**
 * Danh mục quản lý chung một giao diện bảng. Mỗi mục: đường dẫn API quản trị, đường dẫn đọc danh sách,
 * các trường [khóa, nhãn, kiểu, bắt buộc]. Kiểu: text | number | checkbox | method | textarea.
 */
const CATALOGS = {
  regions: { label: 'Khu vực', list: '/api/v1/regions', fields: [['name', 'Tên khu vực', 'text', true]] },
  'tour-categories': { label: 'Loại hình / sở thích', list: '/api/v1/tour-categories', fields: [['name', 'Tên loại hình', 'text', true]] },
  amenities: { label: 'Dịch vụ', list: '/api/v1/amenities', fields: [['name', 'Tên dịch vụ', 'text', true]] },
  'passenger-types': {
    label: 'Loại khách', list: '/api/v1/passenger-types',
    note: 'Khoảng tuổi các loại khách không được chồng nhau. Số chỗ: 1 = ngồi ghế riêng, 0 = ngồi chung (không trừ chỗ).',
    fields: [['name', 'Tên loại khách', 'text', true], ['minAge', 'Tuổi từ', 'number', true], ['maxAge', 'Tuổi đến', 'number', true], ['seats', 'Số chỗ chiếm', 'number', true]],
  },
  'pickup-points': {
    label: 'Điểm đón', list: '/api/v1/admin/pickup-points',
    fields: [['name', 'Tên điểm đón', 'text', true], ['address', 'Địa chỉ', 'text'], ['active', 'Đang dùng', 'checkbox']],
  },
  surcharges: {
    label: 'Phụ thu', list: '/api/v1/admin/surcharges',
    fields: [['name', 'Tên phụ thu', 'text', true], ['method', 'Cách tính', 'method', true], ['unitPrice', 'Đơn giá', 'number', true],
      ['description', 'Mô tả', 'textarea'], ['active', 'Đang áp dụng', 'checkbox']],
    show: { method: (r) => CACH_TINH_PHU_THU[r.method], unitPrice: (r) => money(r.unitPrice) },
  },
}
const TABS = [...Object.entries(CATALOGS).map(([k, c]) => [k, c.label]), ['destinations', 'Điểm đến'], ['cancellation-policies', 'Chính sách hủy']]

export default function Catalogs() {
  const [tab, setTab] = useState('regions')
  return (
    <>
      <div className="admin-head"><h1>Danh mục</h1></div>
      <div className="tabs">
        {TABS.map(([k, l]) => <button key={k} className={tab === k ? 'active' : ''} onClick={() => setTab(k)}>{l}</button>)}
      </div>
      {tab === 'cancellation-policies' ? <Policies />
        : tab === 'destinations' ? <Destinations />
          : <CatalogTable key={tab} path={tab} cfg={CATALOGS[tab]} />}
    </>
  )
}

const show = (v) => (v === true ? '✓' : v === false ? '✗' : String(v ?? '—'))

function CatalogTable({ path, cfg }) {
  const [rows, setRows] = useState(null)
  const [edit, setEdit] = useState(null)
  const [err, setErr] = useState('')
  const load = useCallback(() => api.get(cfg.list).then((r) => setRows(r.data)).catch((e) => setErr(errMsg(e))), [cfg])
  useEffect(() => { load() }, [load])

  const save = async () => {
    setErr('')
    const { id, ...body } = edit
    cfg.fields.forEach(([k, , type]) => {
      if (type === 'number') body[k] = body[k] === '' || body[k] == null ? null : Number(body[k])
      if (type === 'text' || type === 'textarea') body[k] = body[k] === '' ? null : body[k]
    })
    try {
      if (id) await api.put(`/api/v1/admin/${path}/${id}`, body); else await api.post(`/api/v1/admin/${path}`, body)
      setEdit(null); load()
    } catch (e) { setErr(errMsg(e)) }
  }
  const del = async (r) => {
    if (!confirm('Xóa mục này?')) return
    setErr('')
    try { await api.delete(`/api/v1/admin/${path}/${r.id}`); load() } catch (e) { setErr(errMsg(e)) }
  }
  const blank = () => Object.fromEntries(cfg.fields.map(([k, , t]) => [k, t === 'checkbox' ? true : t === 'method' ? 'PER_PERSON' : '']))
  const cols = cfg.fields.filter(([, , t]) => t !== 'textarea')

  const input = ([k, label, type, req]) => {
    const v = edit[k] ?? ''
    const on = (e) => setEdit({ ...edit, [k]: type === 'checkbox' ? e.target.checked : e.target.value })
    if (type === 'checkbox') return <label key={k} className="check field"><input type="checkbox" checked={!!edit[k]} onChange={on} /> {label}</label>
    let el
    if (type === 'textarea') el = <textarea value={v} onChange={on} />
    else if (type === 'method') el = <select value={v} onChange={on}>{Object.entries(CACH_TINH_PHU_THU).map(([a, b]) => <option key={a} value={a}>{b}</option>)}</select>
    else el = <input type={type === 'number' ? 'number' : 'text'} value={v} onChange={on} required={req} />
    return <div key={k} className="field"><label>{label}{req && ' *'}</label>{el}</div>
  }

  return (
    <>
      {cfg.note && <p className="muted small">{cfg.note}</p>}
      <div className="row between" style={{ marginBottom: 12 }}><span className="muted small">{rows?.length ?? 0} mục</span><button className="btn sm" onClick={() => setEdit(blank())}>+ Thêm</button></div>
      <Alert>{err && !edit ? err : ''}</Alert>
      {!rows ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr>{cols.map(([k, l]) => <th key={k}>{l}</th>)}<th></th></tr></thead>
          <tbody>{rows.map((r) => (
            <tr key={r.id}>{cols.map(([k]) => <td key={k}>{cfg.show?.[k] ? cfg.show[k](r) : show(r[k])}</td>)}
              <td className="row"><button className="btn secondary sm" onClick={() => setEdit({ ...r })}>Sửa</button><button className="btn link small" onClick={() => del(r)}>Xóa</button></td></tr>
          ))}</tbody>
        </table></div>
      )}
      {edit && (
        <Modal title={edit.id ? 'Sửa' : 'Thêm mới'} onClose={() => setEdit(null)}>
          {cfg.fields.map(input)}
          <Alert>{err}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" onClick={save}>Lưu</button></div>
        </Modal>
      )}
    </>
  )
}

/* ---------------- Điểm đến ---------------- */

const CHAT_LUONG = { SOURCE: 'Theo nguồn', ALL_DAY: 'Mở cả ngày', ESTIMATED: 'Ước tính', UNVERIFIED: 'Chưa xác minh', UNKNOWN: 'Chưa rõ' }
const TOA_DO = { OSM: 'OpenStreetMap', WIKI: 'Wikipedia', APPROXIMATE: 'Gần đúng' }
const THU = [['MONDAY', 'T2'], ['TUESDAY', 'T3'], ['WEDNESDAY', 'T4'], ['THURSDAY', 'T5'], ['FRIDAY', 'T6'], ['SATURDAY', 'T7'], ['SUNDAY', 'CN']]
const blankDest = {
  code: '', name: '', regionId: '', address: '', latitude: '', longitude: '', coordinateQuality: 'APPROXIMATE', categoryIds: [],
  openTime: '07:00', closeTime: '17:00', closedDays: [], hoursQuality: 'ESTIMATED', ticketPriceAdult: '', ticketPriceChild: '', priceQuality: 'UNKNOWN',
  visitMinutes: 60, popularity: 3, description: '', sources: '', active: true,
}

function Destinations() {
  const [data, setData] = useState(null)
  const [regions, setRegions] = useState([])
  const [categories, setCategories] = useState([])
  const [q, setQ] = useState('')
  const [regionId, setRegionId] = useState('')
  const [page, setPage] = useState(0)
  const [edit, setEdit] = useState(null)
  const [err, setErr] = useState('')

  useEffect(() => {
    api.get('/api/v1/regions').then((r) => setRegions(r.data)).catch(() => {})
    api.get('/api/v1/tour-categories').then((r) => setCategories(r.data)).catch(() => {})
  }, [])
  const load = () => api.get('/api/v1/admin/destinations', { params: { q: q || undefined, regionId: regionId || undefined, page, size: 20 } })
    .then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  useEffect(() => { load() }, [regionId, page]) // eslint-disable-line react-hooks/exhaustive-deps

  const open = (d) => setEdit({
    ...blankDest, ...d, regionId: d.region.id, categoryIds: d.categories.map((c) => c.id), openTime: hhmm(d.openTime), closeTime: hhmm(d.closeTime),
    address: d.address || '', description: d.description || '', sources: d.sources || '', ticketPriceAdult: d.ticketPriceAdult ?? '', ticketPriceChild: d.ticketPriceChild ?? '',
  })
  const set = (k, v) => setEdit({ ...edit, [k]: v })
  const toggle = (k, v) => set(k, edit[k].includes(v) ? edit[k].filter((x) => x !== v) : [...edit[k], v])
  const num = (v) => (v === '' || v == null ? null : Number(v))

  const save = async () => {
    setErr('')
    const { id, region: _r, categories: _c, ...b } = edit
    const body = {
      ...b, regionId: num(b.regionId), latitude: num(b.latitude), longitude: num(b.longitude), visitMinutes: num(b.visitMinutes), popularity: num(b.popularity),
      ticketPriceAdult: num(b.ticketPriceAdult), ticketPriceChild: num(b.ticketPriceChild),
      address: b.address || null, description: b.description || null, sources: b.sources || null,
    }
    try {
      if (id) await api.put(`/api/v1/admin/destinations/${id}`, body); else await api.post('/api/v1/admin/destinations', body)
      setEdit(null); load()
    } catch (e) { setErr(errMsg(e)) }
  }
  const del = async (d) => {
    if (!confirm(`Xóa điểm đến "${d.name}"?`)) return
    try { await api.delete(`/api/v1/admin/destinations/${d.id}`); load() } catch (e) { setErr(errMsg(e)) }
  }

  const sel = (k, map) => <select value={edit[k]} onChange={(e) => set(k, e.target.value)}>{Object.entries(map).map(([a, b]) => <option key={a} value={a}>{b}</option>)}</select>
  const inp = (k, type = 'text', extra = {}) => <input type={type} value={edit[k] ?? ''} onChange={(e) => set(k, e.target.value)} {...extra} />

  return (
    <>
      <p className="muted small">Dữ liệu điểm đến dùng cho lịch trình tour và cho thuật toán gợi ý lịch trình (tọa độ, giờ mở cửa, thời gian tham quan, giá vé, độ nổi bật).</p>
      <div className="row between" style={{ marginBottom: 12 }}>
        <form className="row" onSubmit={(e) => { e.preventDefault(); setPage(0); load() }}>
          <input placeholder="Tìm theo tên, mã" value={q} onChange={(e) => setQ(e.target.value)} style={{ maxWidth: 240 }} />
          <select value={regionId} onChange={(e) => { setPage(0); setRegionId(e.target.value) }} style={{ maxWidth: 200 }}>
            <option value="">Mọi khu vực</option>{regions.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
          <button className="btn secondary sm">Tìm</button>
        </form>
        <button className="btn sm" onClick={() => setEdit({ ...blankDest, regionId: regions[0]?.id || '' })}>+ Thêm điểm đến</button>
      </div>
      <Alert>{err && !edit ? err : ''}</Alert>
      {!data ? <Loading /> : (
        <>
          <div className="table-wrap"><table>
            <thead><tr><th>Mã</th><th>Tên</th><th>Khu vực</th><th>Giờ mở cửa</th><th>Tham quan</th><th className="right">Vé người lớn</th><th>Nổi bật</th><th>Hiện</th><th></th></tr></thead>
            <tbody>{data.items.map((d) => (
              <tr key={d.id}><td>{d.code}</td><td>{d.name}<div className="muted small">{d.categories.map((c) => c.name).join(', ')}</div></td><td className="small">{d.region.name}</td>
                <td className="small">{hhmm(d.openTime)}–{hhmm(d.closeTime)}<div className="muted">{CHAT_LUONG[d.hoursQuality]}</div></td>
                <td>{d.visitMinutes}′</td><td className="right">{d.ticketPriceAdult == null ? '—' : money(d.ticketPriceAdult)}</td><td>{d.popularity}/5</td><td>{show(d.active)}</td>
                <td className="row"><button className="btn secondary sm" onClick={() => open(d)}>Sửa</button><button className="btn link small" onClick={() => del(d)}>Xóa</button></td></tr>
            ))}</tbody>
          </table></div>
          <Pagination page={page} totalPages={pageCount(data)} onChange={setPage} />
        </>
      )}
      {edit && (
        <Modal title={edit.id ? `Sửa điểm đến ${edit.code}` : 'Thêm điểm đến'} onClose={() => setEdit(null)}>
          <div className="grid grid-3">
            <div className="field"><label>Mã *</label>{inp('code')}</div>
            <div className="field" style={{ gridColumn: 'span 2' }}><label>Tên *</label>{inp('name')}</div>
            <div className="field"><label>Khu vực *</label><select value={edit.regionId} onChange={(e) => set('regionId', e.target.value)}>{regions.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}</select></div>
            <div className="field" style={{ gridColumn: 'span 2' }}><label>Địa chỉ</label>{inp('address')}</div>
            <div className="field"><label>Vĩ độ *</label>{inp('latitude', 'number', { step: 'any' })}</div>
            <div className="field"><label>Kinh độ *</label>{inp('longitude', 'number', { step: 'any' })}</div>
            <div className="field"><label>Độ chính xác tọa độ</label>{sel('coordinateQuality', TOA_DO)}</div>
            <div className="field"><label>Giờ mở cửa *</label>{inp('openTime', 'time')}</div>
            <div className="field"><label>Giờ đóng cửa *</label>{inp('closeTime', 'time')}</div>
            <div className="field"><label>Nguồn giờ mở cửa</label>{sel('hoursQuality', CHAT_LUONG)}</div>
            <div className="field"><label>Vé người lớn (đ)</label>{inp('ticketPriceAdult', 'number')}</div>
            <div className="field"><label>Vé trẻ em (đ)</label>{inp('ticketPriceChild', 'number')}</div>
            <div className="field"><label>Nguồn giá vé</label>{sel('priceQuality', CHAT_LUONG)}</div>
            <div className="field"><label>Thời gian tham quan (phút) *</label>{inp('visitMinutes', 'number', { min: 1 })}</div>
            <div className="field"><label>Độ nổi bật (1–5) *</label>{inp('popularity', 'number', { min: 1, max: 5 })}</div>
            <div className="field"><label>&nbsp;</label><label className="check"><input type="checkbox" checked={!!edit.active} onChange={(e) => set('active', e.target.checked)} /> Đang hiển thị</label></div>
          </div>
          <div className="field"><label>Ngày đóng cửa trong tuần</label>
            <div className="row">{THU.map(([k, l]) => <label key={k} className="check"><input type="checkbox" checked={edit.closedDays.includes(k)} onChange={() => toggle('closedDays', k)} />{l}</label>)}</div></div>
          <div className="field"><label>Thẻ sở thích *</label>
            <div className="row" style={{ flexWrap: 'wrap' }}>{categories.map((c) => <label key={c.id} className="check"><input type="checkbox" checked={edit.categoryIds.includes(c.id)} onChange={() => toggle('categoryIds', c.id)} />{c.name}</label>)}</div></div>
          <div className="field"><label>Mô tả</label><textarea value={edit.description} onChange={(e) => set('description', e.target.value)} /></div>
          <div className="field"><label>Nguồn dữ liệu</label>{inp('sources')}</div>
          <Alert>{err}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" onClick={save}>Lưu</button></div>
        </Modal>
      )}
    </>
  )
}

/* ---------------- Chính sách hủy ---------------- */

function Policies() {
  const [rows, setRows] = useState(null)
  const [edit, setEdit] = useState(null)
  const [err, setErr] = useState('')
  const load = () => api.get('/api/v1/admin/cancellation-policies').then((r) => setRows(r.data)).catch((e) => setErr(errMsg(e)))
  useEffect(() => { load() }, [])
  const today = new Date().toISOString().slice(0, 10)

  const save = async () => {
    setErr('')
    const { id, ...b } = edit
    const body = { ...b, description: b.description || null,
      tiers: b.tiers.map((m) => ({ daysBeforeFrom: Number(m.daysBeforeFrom), daysBeforeTo: Number(m.daysBeforeTo), penaltyPercent: Number(m.penaltyPercent) })) }
    try {
      if (id) await api.put(`/api/v1/admin/cancellation-policies/${id}`, body)
      else await api.post('/api/v1/admin/cancellation-policies', body)
      setEdit(null); load()
    } catch (e) { setErr(errMsg(e)) }
  }
  const del = async (c) => {
    if (!confirm('Xóa chính sách?')) return
    try { await api.delete(`/api/v1/admin/cancellation-policies/${c.id}`); load() } catch (e) { setErr(errMsg(e)) }
  }
  const setM = (i, k, v) => setEdit({ ...edit, tiers: edit.tiers.map((m, j) => (j === i ? { ...m, [k]: v } : m)) })

  return (
    <>
      <div className="row between" style={{ marginBottom: 12 }}>
        <span className="muted small">Đơn đặt chốt chính sách đang hiệu lực tại thời điểm đặt. Chính sách đã có hiệu lực thì không sửa/xóa được; muốn đổi quy tắc hãy tạo chính sách mới với ngày hiệu lực mới.</span>
        <button className="btn sm" onClick={() => setEdit({ name: '', effectiveFrom: today, description: '', tiers: [{ daysBeforeFrom: 0, daysBeforeTo: 365, penaltyPercent: 0 }] })}>+ Thêm chính sách</button>
      </div>
      {!rows ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Tên</th><th>Hiệu lực từ</th><th>Mốc phạt</th><th></th></tr></thead>
          <tbody>{rows.map((c) => {
            const locked = c.effectiveFrom <= today
            return (
              <tr key={c.id}><td>{c.name}{c.description && <div className="muted small">{c.description}</div>}</td>
                <td className="small">{fmtDate(c.effectiveFrom)}{locked && <div className="muted">Đã hiệu lực</div>}</td>
                <td className="small">{c.tiers.map((m, i) => <div key={i}>{tenMocPhat(m)}: {m.penaltyPercent}%</div>)}</td>
                <td className="row">{!locked && <><button className="btn secondary sm" onClick={() => setEdit({ ...c, description: c.description || '' })}>Sửa</button>
                  <button className="btn link small" onClick={() => del(c)}>Xóa</button></>}</td></tr>
            )
          })}</tbody>
        </table></div>
      )}
      <Alert>{!edit ? err : ''}</Alert>
      {edit && (
        <Modal title="Chính sách hủy" onClose={() => setEdit(null)}>
          <div className="grid grid-2">
            <div className="field"><label>Tên *</label><input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} /></div>
            <div className="field"><label>Hiệu lực từ ngày *</label><input type="date" value={edit.effectiveFrom} onChange={(e) => setEdit({ ...edit, effectiveFrom: e.target.value })} /></div>
          </div>
          <div className="field"><label>Mô tả</label><textarea value={edit.description} onChange={(e) => setEdit({ ...edit, description: e.target.value })} /></div>
          <h4>Mốc phạt (số ngày hủy trước ngày khởi hành)</h4>
          {edit.tiers.map((m, i) => (
            <div key={i} className="row" style={{ marginBottom: 6 }}>
              Từ <input type="number" min="0" style={{ width: 90 }} value={m.daysBeforeFrom} onChange={(e) => setM(i, 'daysBeforeFrom', e.target.value)} />
              đến <input type="number" min="0" style={{ width: 90 }} value={m.daysBeforeTo} onChange={(e) => setM(i, 'daysBeforeTo', e.target.value)} />
              ngày → phạt <input type="number" min="0" max="100" style={{ width: 80 }} value={m.penaltyPercent} onChange={(e) => setM(i, 'penaltyPercent', e.target.value)} />%
              <button className="btn link" onClick={() => setEdit({ ...edit, tiers: edit.tiers.filter((_, j) => j !== i) })}>✕</button>
            </div>
          ))}
          <button className="btn secondary sm" onClick={() => setEdit({ ...edit, tiers: [...edit.tiers, { daysBeforeFrom: 0, daysBeforeTo: 0, penaltyPercent: 100 }] })}>+ Mốc</button>
          <Alert>{err}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end', marginTop: 12 }}><button className="btn" onClick={save}>Lưu</button></div>
        </Modal>
      )}
    </>
  )
}
