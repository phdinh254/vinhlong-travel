import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import api, { errMsg, imgUrl, uploadImage, money, fmtDateTime, toLocalInput, hhmm, nhanDot, TRANG_THAI_DOT, CACH_TINH_PHU_THU } from '../../api'
import { Alert, Loading, Modal } from '../../components/ui.jsx'
import ItineraryEditor, { toEditable, toPayload } from '../../components/ItineraryEditor.jsx'

const emptyTour = {
  code: '', name: '', summary: '', description: '', durationDays: 1, durationNights: 0, status: 'DRAFT', featured: false,
  regionIds: [], categoryIds: [], amenities: [], surchargeIds: [], images: [], days: [],
}

/** Tải danh mục cần cho form tour và form đợt */
async function loadAdminCatalogs() {
  const get = (u, params) => api.get(u, { params }).then((r) => r.data)
  const [regions, categories, amenities, dest, passengerTypes, pickupPoints, surcharges] = await Promise.all([
    get('/api/v1/regions'), get('/api/v1/tour-categories'), get('/api/v1/amenities'), get('/api/v1/admin/destinations', { size: 100 }),
    get('/api/v1/passenger-types'), get('/api/v1/admin/pickup-points'), get('/api/v1/admin/surcharges'),
  ])
  return { regions, categories, amenities, destinations: dest.items.sort((a, b) => a.name.localeCompare(b.name, 'vi')), passengerTypes, pickupPoints, surcharges }
}

export default function TourForm() {
  const { id } = useParams()
  const nav = useNavigate()
  const [tab, setTab] = useState('info')
  const [f, setF] = useState(emptyTour)
  const [dots, setDots] = useState([])
  const [dm, setDm] = useState(null)
  const [err, setErr] = useState('')
  const [msg, setMsg] = useState('')
  const [busy, setBusy] = useState(false)
  const [dotEdit, setDotEdit] = useState(null)
  const [dotLT, setDotLT] = useState(null)

  useEffect(() => { loadAdminCatalogs().then(setDm).catch((e) => setErr(errMsg(e))) }, [])

  const fill = (t) => setF({
    code: t.code, name: t.name, summary: t.summary || '', description: t.description || '', durationDays: t.durationDays, durationNights: t.durationNights,
    status: t.status, featured: t.featured, regionIds: t.regions.map((x) => x.id), categoryIds: t.categories.map((x) => x.id),
    amenities: [...t.includedAmenities.map((a) => ({ amenityId: a.id, included: true })), ...t.excludedAmenities.map((a) => ({ amenityId: a.id, included: false }))],
    surchargeIds: t.surcharges.map((s) => s.id), images: t.images.map((h) => ({ url: h.url, caption: h.caption || '', cover: h.cover })),
    days: toEditable(t.itinerary?.days),
  })
  const loadDots = () => api.get(`/api/v1/admin/tours/${id}/departures`).then((r) => setDots(r.data)).catch((e) => setErr(errMsg(e)))
  const loadTour = () => api.get(`/api/v1/admin/tours/${id}`).then((r) => { fill(r.data); loadDots() }).catch((e) => setErr(errMsg(e)))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { if (id) loadTour() }, [id])

  const toggle = (k, v) => setF({ ...f, [k]: f[k].includes(v) ? f[k].filter((x) => x !== v) : [...f[k], v] })

  const tourBody = (status) => ({
    code: f.code, name: f.name, summary: f.summary || null, description: f.description || null,
    durationDays: Number(f.durationDays), durationNights: Number(f.durationNights),
    regionIds: f.regionIds, categoryIds: f.categoryIds, amenities: f.amenities, surchargeIds: f.surchargeIds, featured: f.featured, status,
  })

  /** Lưu theo thứ tự: tour mới tạo ở dạng nháp → ảnh → lịch trình → thông tin và trạng thái (mở bán cần đủ ảnh và lịch trình) */
  const save = async () => {
    setBusy(true); setErr(''); setMsg('')
    try {
      const tourId = id || (await api.post('/api/v1/admin/tours', tourBody('DRAFT'))).data.id
      await api.put(`/api/v1/admin/tours/${tourId}/images`, { images: f.images.map((h) => ({ url: h.url, caption: h.caption || null, cover: h.cover })) })
      await api.put(`/api/v1/admin/tours/${tourId}/itinerary`, toPayload(f.days))
      const { data } = await api.put(`/api/v1/admin/tours/${tourId}`, tourBody(f.status))
      fill(data)
      setMsg('Đã lưu tour')
      if (!id) nav(`/admin/tours/${tourId}`, { replace: true })
    } catch (e) { setErr(errMsg(e)) } finally { setBusy(false) }
  }

  const addImages = async (e) => {
    const files = [...e.target.files]
    try {
      const urls = await Promise.all(files.map(uploadImage))
      setF((p) => ({ ...p, images: [...p.images, ...urls.map((u) => ({ url: u, caption: '', cover: false }))] }))
    } catch (x) { setErr(errMsg(x)) }
    e.target.value = ''
  }

  const setAmenity = (amenityId, v) => {
    const rest = f.amenities.filter((d) => d.amenityId !== amenityId)
    setF({ ...f, amenities: v === '' ? rest : [...rest, { amenityId, included: v === 'IN' }] })
  }

  const delDot = async (d) => {
    if (!confirm(`Xóa đợt ${d.code}?`)) return
    try { await api.delete(`/api/v1/admin/departures/${d.id}`); loadDots() } catch (e) { setErr(errMsg(e)) }
  }

  if (!dm || (id && !f.code && !err)) return err ? <Alert>{err}</Alert> : <Loading />

  return (
    <>
      <div className="admin-head">
        <h1>{id ? `Sửa tour ${f.code}` : 'Thêm tour'}</h1>
        <div className="row"><Link className="btn secondary" to="/admin/tours">← Danh sách</Link><button className="btn" disabled={busy} onClick={save}>{busy ? 'Đang lưu…' : 'Lưu tour'}</button></div>
      </div>
      <Alert>{err}</Alert><Alert type="ok">{msg}</Alert>
      <div className="tabs">
        {[['info', 'Thông tin'], ['img', `Hình ảnh (${f.images.length})`], ['dv', 'Dịch vụ & phụ thu'], ['lt', `Lịch trình mẫu (${f.days.length}/${f.durationDays} ngày)`], ...(id ? [['dot', `Đợt khởi hành (${dots.length})`]] : [])]
          .map(([k, l]) => <button key={k} className={tab === k ? 'active' : ''} onClick={() => setTab(k)}>{l}</button>)}
      </div>

      {tab === 'info' && (
        <div className="card">
          <div className="grid grid-4">
            <div className="field"><label>Mã tour *</label><input value={f.code} onChange={(e) => setF({ ...f, code: e.target.value })} /></div>
            <div className="field" style={{ gridColumn: 'span 3' }}><label>Tên tour *</label><input value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></div>
            <div className="field"><label>Số ngày *</label><input type="number" min="1" value={f.durationDays} onChange={(e) => setF({ ...f, durationDays: e.target.value })} /></div>
            <div className="field"><label>Số đêm *</label><input type="number" min="0" value={f.durationNights} onChange={(e) => setF({ ...f, durationNights: e.target.value })} /></div>
            <div className="field"><label>Trạng thái</label><select value={f.status} onChange={(e) => setF({ ...f, status: e.target.value })}><option value="DRAFT">Nháp</option><option value="ON_SALE">Đang bán</option><option value="STOPPED">Ngừng bán</option></select></div>
            <div className="field"><label>&nbsp;</label><label className="check"><input type="checkbox" checked={f.featured} onChange={(e) => setF({ ...f, featured: e.target.checked })} /> Tour nổi bật</label></div>
          </div>
          <p className="muted small">Mở bán cần có ít nhất một ảnh và lịch trình mẫu đủ số ngày.</p>
          <div className="field"><label>Tóm tắt</label><input value={f.summary} onChange={(e) => setF({ ...f, summary: e.target.value })} /></div>
          <div className="field"><label>Mô tả (cho phép HTML)</label><textarea style={{ minHeight: 160 }} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></div>
          <div className="grid grid-2">
            <div className="field"><label>Loại hình du lịch *</label><div className="box">{dm.categories.map((x) => <label key={x.id} className="check"><input type="checkbox" checked={f.categoryIds.includes(x.id)} onChange={() => toggle('categoryIds', x.id)} />{x.name}</label>)}</div></div>
            <div className="field"><label>Khu vực *</label><div className="box">{dm.regions.map((x) => <label key={x.id} className="check"><input type="checkbox" checked={f.regionIds.includes(x.id)} onChange={() => toggle('regionIds', x.id)} />{x.name}</label>)}</div></div>
          </div>
        </div>
      )}

      {tab === 'img' && (
        <div className="card">
          <p className="muted small">Ảnh có viền vàng là ảnh đại diện (không chọn thì ảnh đầu tiên được dùng). Thứ tự hiển thị theo thứ tự bên dưới.</p>
          <div className="thumbs">
            {f.images.map((h, i) => (
              <div key={i} className={`t ${h.cover ? 'main' : ''}`}>
                <div style={{ backgroundImage: `url(${imgUrl(h.url)})` }} />
                <div className="row small" style={{ gap: 4, marginTop: 4 }}>
                  <button className="btn link small" onClick={() => setF({ ...f, images: f.images.map((x, j) => ({ ...x, cover: j === i })) })}>Đại diện</button>
                  <button className="btn link small" disabled={i === 0} onClick={() => { const a = [...f.images]; [a[i - 1], a[i]] = [a[i], a[i - 1]]; setF({ ...f, images: a }) }}>←</button>
                  <button className="btn link small" onClick={() => setF({ ...f, images: f.images.filter((_, j) => j !== i) })}>Xóa</button>
                </div>
              </div>
            ))}
          </div>
          <label className="btn secondary" style={{ marginTop: 14 }}>+ Tải ảnh lên<input type="file" accept="image/*" multiple hidden onChange={addImages} /></label>
        </div>
      )}

      {tab === 'dv' && (
        <div className="grid grid-2">
          <div className="card">
            <h3>Dịch vụ</h3>
            <div className="table-wrap"><table>
              <thead><tr><th>Dịch vụ</th><th>Áp dụng</th></tr></thead>
              <tbody>{dm.amenities.map((d) => {
                const cur = f.amenities.find((x) => x.amenityId === d.id)
                return (
                  <tr key={d.id}><td>{d.name}</td>
                    <td><select value={cur ? (cur.included ? 'IN' : 'OUT') : ''} onChange={(e) => setAmenity(d.id, e.target.value)}>
                      <option value="">— Không đề cập —</option><option value="IN">Bao gồm</option><option value="OUT">Không bao gồm</option></select></td></tr>
                )
              })}</tbody>
            </table></div>
          </div>
          <div className="card">
            <h3>Phụ thu khách được chọn</h3>
            {dm.surcharges.length === 0 && <p className="muted small">Chưa có phụ thu. Thêm ở mục Danh mục.</p>}
            {dm.surcharges.map((s) => (
              <label key={s.id} className="check"><input type="checkbox" checked={f.surchargeIds.includes(s.id)} onChange={() => toggle('surchargeIds', s.id)} />
                {s.name} <span className="muted small">({CACH_TINH_PHU_THU[s.method]} · {money(s.unitPrice)}){!s.active && ' · đang tắt'}</span></label>
            ))}
          </div>
        </div>
      )}

      {tab === 'lt' && (
        <div className="card">
          <p className="muted small">Lịch trình mẫu của tour. Mỗi đợt khởi hành mới tự sao chép lịch trình này để chỉnh riêng; sửa lịch trình mẫu không làm đổi các đợt đã tạo.</p>
          <ItineraryEditor value={f.days} onChange={(v) => setF({ ...f, days: v })} destinations={dm.destinations} />
        </div>
      )}

      {tab === 'dot' && id && (
        <div className="card">
          <div className="row between" style={{ marginBottom: 12 }}><h3 style={{ margin: 0 }}>Đợt khởi hành</h3><button className="btn sm" onClick={() => setDotEdit({})}>+ Thêm đợt</button></div>
          <div className="table-wrap"><table>
            <thead><tr><th>Mã đợt</th><th>Khởi hành</th><th>Về</th><th>Chỗ</th><th>Bảng giá</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>{dots.map((d) => {
              const [label, cls] = nhanDot(d)
              return (
                <tr key={d.id}>
                  <td>{d.code}</td><td>{fmtDateTime(d.departureAt)}<div className="muted small">Hạn nhận: {fmtDateTime(d.bookingDeadline)}</div></td><td>{fmtDateTime(d.returnAt)}</td>
                  <td>{d.bookedSeats}/{d.capacity}</td>
                  <td className="small">{d.prices.map((g) => <div key={g.passengerTypeId}>{g.passengerTypeName}: {money(g.price)}</div>)}</td>
                  <td><span className={`badge ${cls}`}>{label}</span></td>
                  <td>
                    <button className="btn secondary sm" onClick={() => setDotEdit(d)}>Sửa</button>{' '}
                    <button className="btn link small" onClick={() => setDotLT(d)}>Lịch trình</button>{' '}
                    <Link className="btn link small" to={`/admin/don-dat?departureId=${d.id}`}>Đơn</Link>{' '}
                    <button className="btn link small" onClick={() => delDot(d)}>Xóa</button>
                  </td>
                </tr>
              )
            })}</tbody>
          </table></div>
        </div>
      )}

      {dotEdit && <DotForm tourId={Number(id)} dot={dotEdit} dm={dm} onClose={() => setDotEdit(null)} onSaved={() => { setDotEdit(null); loadDots() }} />}
      {dotLT && <DotItinerary dot={dotLT} dm={dm} onClose={() => setDotLT(null)} onSaved={() => setDotLT(null)} />}
    </>
  )
}

/** Form đợt khởi hành + bảng giá theo loại khách + điểm đón */
function DotForm({ tourId, dot, dm, onClose, onSaved }) {
  const isNew = !dot.id
  const [f, setF] = useState({
    departureAt: toLocalInput(dot.departureAt), returnAt: toLocalInput(dot.returnAt), bookingDeadline: toLocalInput(dot.bookingDeadline),
    capacity: dot.capacity || 30, status: dot.status || 'OPEN',
  })
  const [gia, setGia] = useState(() => Object.fromEntries(dm.passengerTypes.map((l) => [l.id, dot.prices?.find((g) => g.passengerTypeId === l.id)?.price ?? ''])))
  const [dds, setDds] = useState(dot.pickups?.map((d) => ({ pickupPointId: d.pickupPointId, pickupTime: hhmm(d.pickupTime), fee: d.fee })) || [])
  const [err, setErr] = useState('')

  const save = async () => {
    setErr('')
    if (Object.values(gia).some((v) => v === '' || v == null)) { setErr('Cần nhập giá cho mọi loại khách (nhập 0 nếu miễn phí).'); return }
    try {
      const body = {
        ...f, capacity: Number(f.capacity),
        prices: Object.entries(gia).map(([k, v]) => ({ passengerTypeId: Number(k), price: Number(v) })),
        pickups: dds.filter((d) => d.pickupPointId && d.pickupTime).map((d) => ({ pickupPointId: Number(d.pickupPointId), pickupTime: d.pickupTime, fee: Number(d.fee) || 0 })),
      }
      if (isNew) await api.post(`/api/v1/admin/tours/${tourId}/departures`, body); else await api.put(`/api/v1/admin/departures/${dot.id}`, body)
      onSaved()
    } catch (e) { setErr(errMsg(e)) }
  }

  return (
    <Modal title={isNew ? 'Thêm đợt khởi hành' : `Sửa đợt ${dot.code}`} onClose={onClose}>
      <div className="grid grid-3">
        <div className="field"><label>Ngày giờ đi *</label><input type="datetime-local" value={f.departureAt} onChange={(e) => setF({ ...f, departureAt: e.target.value })} /></div>
        <div className="field"><label>Ngày giờ về *</label><input type="datetime-local" value={f.returnAt} onChange={(e) => setF({ ...f, returnAt: e.target.value })} /></div>
        <div className="field"><label>Hạn nhận khách *</label><input type="datetime-local" value={f.bookingDeadline} onChange={(e) => setF({ ...f, bookingDeadline: e.target.value })} /></div>
        <div className="field"><label>Sức chứa *</label><input type="number" min="1" value={f.capacity} onChange={(e) => setF({ ...f, capacity: e.target.value })} /></div>
        <div className="field"><label>Trạng thái</label><select value={f.status} onChange={(e) => setF({ ...f, status: e.target.value })}>{Object.entries(TRANG_THAI_DOT).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}</select></div>
      </div>
      {isNew && <p className="muted small">Mã đợt được tạo tự động; đợt mới sao chép lịch trình mẫu của tour.</p>}
      <h4>Bảng giá theo loại khách</h4>
      <div className="grid grid-3">
        {dm.passengerTypes.map((l) => (
          <div key={l.id} className="field"><label>{l.name} ({l.minAge}–{l.maxAge} tuổi)</label>
            <input type="number" min="0" step="1000" value={gia[l.id]} onChange={(e) => setGia({ ...gia, [l.id]: e.target.value })} /></div>
        ))}
      </div>
      <h4>Điểm đón</h4>
      {dds.map((d, i) => (
        <div key={i} className="row" style={{ marginBottom: 6 }}>
          <select className="grow" style={{ width: 'auto' }} value={d.pickupPointId} onChange={(e) => setDds(dds.map((x, j) => (j === i ? { ...x, pickupPointId: e.target.value } : x)))}>
            <option value="">— Chọn điểm đón —</option>{dm.pickupPoints.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
          </select>
          <input type="time" style={{ width: 120 }} value={d.pickupTime} onChange={(e) => setDds(dds.map((x, j) => (j === i ? { ...x, pickupTime: e.target.value } : x)))} />
          <input type="number" min="0" step="1000" style={{ width: 140 }} placeholder="Phí đón/khách" value={d.fee} onChange={(e) => setDds(dds.map((x, j) => (j === i ? { ...x, fee: e.target.value } : x)))} />
          <button className="btn link" onClick={() => setDds(dds.filter((_, j) => j !== i))}>✕</button>
        </div>
      ))}
      <button className="btn secondary sm" onClick={() => setDds([...dds, { pickupPointId: '', pickupTime: f.departureAt.slice(11, 16), fee: 0 }])}>+ Điểm đón</button>
      <Alert>{err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 14 }}><button className="btn secondary" onClick={onClose}>Hủy</button><button className="btn" onClick={save}>Lưu đợt</button></div>
    </Modal>
  )
}

/** Lịch trình riêng của một đợt (bản sao lịch trình mẫu khi tạo đợt) */
function DotItinerary({ dot, dm, onClose, onSaved }) {
  const [v, setV] = useState(null)
  const [err, setErr] = useState('')
  useEffect(() => {
    api.get(`/api/v1/admin/departures/${dot.id}/itinerary`).then((r) => setV(toEditable(r.data.days))).catch((e) => setErr(errMsg(e)))
  }, [dot])
  const save = async () => {
    try { await api.put(`/api/v1/admin/departures/${dot.id}/itinerary`, toPayload(v)); onSaved() } catch (e) { setErr(errMsg(e)) }
  }
  return (
    <Modal title={`Lịch trình đợt ${dot.code}`} onClose={onClose}>
      <p className="muted small">Chỉnh lịch trình riêng của đợt này; không ảnh hưởng lịch trình mẫu của tour.</p>
      {!v ? (err ? null : <Loading />) : <ItineraryEditor value={v} onChange={setV} destinations={dm.destinations} />}
      <Alert>{err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 14 }}>
        <button className="btn" disabled={!v} onClick={save}>Lưu lịch trình đợt</button>
      </div>
    </Modal>
  )
}
