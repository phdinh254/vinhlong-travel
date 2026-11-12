import { useEffect, useState, useCallback } from 'react'
import api, { errMsg, money, fmtDate, CACH_TINH_PHU_THU } from '../../api'
import { Alert, Loading, Modal, ImagePicker } from '../../components/ui.jsx'

/** Cấu hình các danh mục quản lý chung một giao diện */
const CATALOGS = {
  'loai-hinh': { label: 'Loại hình (Q02)', id: 'loaiHinhId',
    fields: [['tenLoaiHinh', 'Tên loại hình', 'text', true], ['moTa', 'Mô tả']] },
  'khu-vuc': { label: 'Khu vực (Q03)', id: 'khuVucId',
    fields: [['tenKhuVuc', 'Tên khu vực', 'text', true], ['moTa', 'Mô tả']] },
  'diem-den': { label: 'Điểm đến (Q04–Q05)', id: 'diemDenId',
    fields: [['tenDiemDen', 'Tên điểm đến', 'text', true], ['khuVucId', 'Khu vực', 'khuVuc'], ['diaChi', 'Địa chỉ'],
      ['viDo', 'Vĩ độ', 'number'], ['kinhDo', 'Kinh độ', 'number'], ['moTa', 'Mô tả', 'textarea'], ['hinhAnh', 'Hình ảnh', 'image']],
    toForm: (r) => ({ ...r, khuVucId: r.khuVuc?.khuVucId || '' }),
    show: { khuVucId: (r) => r.khuVuc?.tenKhuVuc || '—' } },
  'dich-vu': { label: 'Dịch vụ (Q07)', id: 'dichVuId',
    fields: [['tenDichVu', 'Tên dịch vụ', 'text', true], ['bieuTuong', 'Biểu tượng (mã)']] },
  'loai-khach': { label: 'Loại khách (Q16–Q19)', id: 'loaiKhachId',
    fields: [['tenLoaiKhach', 'Tên loại khách', 'text', true], ['tuoiTu', 'Tuổi từ', 'number', true], ['tuoiDen', 'Tuổi đến', 'number', true],
      ['heSoChiemCho', 'Hệ số chiếm chỗ', 'heSo', true]],
    show: { heSoChiemCho: (r) => (r.heSoChiemCho === 1 ? '1 – ghế riêng' : '0 – ngồi chung') } },
  'diem-don': { label: 'Điểm đón (Q20)', id: 'diemDonId',
    fields: [['tenDiemDon', 'Tên điểm đón', 'text', true], ['diaChi', 'Địa chỉ'], ['khuVucId', 'Khu vực', 'khuVuc']],
    toForm: (r) => ({ ...r, khuVucId: r.khuVuc?.khuVucId || '' }),
    show: { khuVucId: (r) => r.khuVuc?.tenKhuVuc || '—' } },
  'phu-thu': { label: 'Phụ thu (Q21)', id: 'phuThuId',
    fields: [['tenPhuThu', 'Tên phụ thu', 'text', true], ['tourId', 'Áp dụng cho tour', 'tour'], ['cachTinh', 'Cách tính', 'cachTinh', true],
      ['donGia', 'Đơn giá', 'number', true], ['conHieuLuc', 'Còn hiệu lực', 'checkbox']],
    show: { tourId: (r, ctx) => ctx.tours.find((t) => t.tourId === r.tourId)?.tenTour || 'Tất cả tour', cachTinh: (r) => CACH_TINH_PHU_THU[r.cachTinh], donGia: (r) => money(r.donGia), conHieuLuc: (r) => (r.conHieuLuc ? '✓' : '✗') } },
}

export default function Catalogs() {
  const [tab, setTab] = useState('loai-hinh')
  const [ctx, setCtx] = useState({ khuVucs: [], tours: [] })
  const loadCtx = useCallback(() => Promise.all([api.get('/api/admin/danh-muc/khu-vuc'), api.get('/api/admin/tours', { params: { size: 100 } })])
    .then(([k, t]) => setCtx({ khuVucs: k.data, tours: t.data.content })), [])
  useEffect(() => { loadCtx() }, [loadCtx])
  return (
    <>
      <div className="admin-head"><h1>Danh mục</h1></div>
      <div className="tabs">
        {[...Object.entries(CATALOGS).map(([k, c]) => [k, c.label]), ['chinh-sach-huy', 'Chính sách hủy (Q37)']]
          .map(([k, l]) => <button key={k} className={tab === k ? 'active' : ''} onClick={() => setTab(k)}>{l}</button>)}
      </div>
      {tab === 'chinh-sach-huy' ? <Policies tours={ctx.tours} /> : <CatalogTable key={tab} path={tab} cfg={CATALOGS[tab]} ctx={ctx} onChanged={tab === 'khu-vuc' ? loadCtx : undefined} />}
    </>
  )
}

function CatalogTable({ path, cfg, ctx, onChanged }) {
  const [rows, setRows] = useState(null)
  const [edit, setEdit] = useState(null)
  const [err, setErr] = useState('')
  const load = useCallback(() => api.get(`/api/admin/danh-muc/${path}`).then((r) => setRows(r.data)), [path])
  useEffect(() => { load() }, [load])

  const save = async () => {
    setErr('')
    const body = { ...edit }
    cfg.fields.forEach(([k, , type]) => {
      if (['number', 'heSo'].includes(type)) body[k] = body[k] === '' || body[k] == null ? null : Number(body[k])
      if (['khuVuc', 'tour'].includes(type)) body[k] = body[k] ? Number(body[k]) : null
    })
    try {
      const id = edit[cfg.id]
      if (id) await api.put(`/api/admin/danh-muc/${path}/${id}`, body); else await api.post(`/api/admin/danh-muc/${path}`, body)
      setEdit(null); load(); onChanged?.()
    } catch (e) { setErr(errMsg(e)) }
  }
  const del = async (r) => {
    if (!confirm('Xóa mục này?')) return
    try { await api.delete(`/api/admin/danh-muc/${path}/${r[cfg.id]}`); load(); onChanged?.() } catch (e) { setErr(errMsg(e)) }
  }
  const blank = () => Object.fromEntries(cfg.fields.map(([k, , t]) => [k, t === 'checkbox' ? true : t === 'heSo' ? 1 : t === 'cachTinh' ? 'THEO_NGUOI' : '']))
  const cols = cfg.fields.filter(([, , t]) => !['textarea', 'image'].includes(t))

  const input = ([k, label, type, req]) => {
    const v = edit[k] ?? ''
    const on = (e) => setEdit({ ...edit, [k]: type === 'checkbox' ? e.target.checked : e.target.value })
    if (type === 'image') return <ImagePicker key={k} label={label} value={v} onChange={(x) => setEdit({ ...edit, [k]: x })} />
    let el
    if (type === 'textarea') el = <textarea value={v} onChange={on} />
    else if (type === 'checkbox') return <label key={k} className="check field"><input type="checkbox" checked={!!edit[k]} onChange={on} /> {label}</label>
    else if (type === 'khuVuc') el = <select value={v} onChange={on}><option value="">— Không —</option>{ctx.khuVucs.map((x) => <option key={x.khuVucId} value={x.khuVucId}>{x.tenKhuVuc}</option>)}</select>
    else if (type === 'tour') el = <select value={v} onChange={on}><option value="">Tất cả tour</option>{ctx.tours.map((x) => <option key={x.tourId} value={x.tourId}>{x.tenTour}</option>)}</select>
    else if (type === 'cachTinh') el = <select value={v} onChange={on}>{Object.entries(CACH_TINH_PHU_THU).map(([a, b]) => <option key={a} value={a}>{b}</option>)}</select>
    else if (type === 'heSo') el = <select value={v} onChange={on}><option value={1}>1 – ngồi ghế riêng</option><option value={0}>0 – ngồi chung (không trừ chỗ)</option></select>
    else el = <input type={type === 'number' ? 'number' : 'text'} step="any" value={v} onChange={on} required={req} />
    return <div key={k} className="field"><label>{label}{req && ' *'}</label>{el}</div>
  }

  return (
    <>
      <div className="row between" style={{ marginBottom: 12 }}><span className="muted small">{rows?.length ?? 0} mục</span><button className="btn sm" onClick={() => setEdit(blank())}>+ Thêm</button></div>
      <Alert>{err && !edit ? err : ''}</Alert>
      {!rows ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr>{cols.map(([k, l]) => <th key={k}>{l}</th>)}<th></th></tr></thead>
          <tbody>{rows.map((r) => (
            <tr key={r[cfg.id]}>{cols.map(([k]) => <td key={k}>{cfg.show?.[k] ? cfg.show[k](r, ctx) : String(r[k] ?? '—')}</td>)}
              <td className="row"><button className="btn secondary sm" onClick={() => setEdit(cfg.toForm ? cfg.toForm(r) : { ...r })}>Sửa</button><button className="btn link small" onClick={() => del(r)}>Xóa</button></td></tr>
          ))}</tbody>
        </table></div>
      )}
      {edit && (
        <Modal title={edit[cfg.id] ? 'Sửa' : 'Thêm mới'} onClose={() => setEdit(null)}>
          {cfg.fields.map(input)}
          <Alert>{err}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" onClick={save}>Lưu</button></div>
        </Modal>
      )}
    </>
  )
}

/** Chính sách hủy có phiên bản + các mốc phạt (Q37) */
function Policies({ tours }) {
  const [rows, setRows] = useState(null)
  const [edit, setEdit] = useState(null)
  const [err, setErr] = useState('')
  const load = () => api.get('/api/admin/danh-muc/chinh-sach-huy').then((r) => setRows(r.data))
  useEffect(() => { load() }, [])

  const save = async () => {
    setErr('')
    const body = { ...edit, tourId: edit.tourId ? Number(edit.tourId) : null, phienBan: Number(edit.phienBan), denNgay: edit.denNgay || null,
      mocPhats: edit.mocPhats.map((m) => ({ soNgayTruocTu: Number(m.soNgayTruocTu), soNgayTruocDen: Number(m.soNgayTruocDen), phanTramPhat: Number(m.phanTramPhat) })) }
    try {
      if (edit.chinhSachHuyId) await api.put(`/api/admin/danh-muc/chinh-sach-huy/${edit.chinhSachHuyId}`, body)
      else await api.post('/api/admin/danh-muc/chinh-sach-huy', body)
      setEdit(null); load()
    } catch (e) { setErr(errMsg(e)) }
  }
  const setM = (i, k, v) => setEdit({ ...edit, mocPhats: edit.mocPhats.map((m, j) => (j === i ? { ...m, [k]: v } : m)) })

  return (
    <>
      <div className="row between" style={{ marginBottom: 12 }}>
        <span className="muted small">Đơn đặt lưu cố định phiên bản chính sách tại thời điểm đặt. Muốn đổi quy tắc, hãy tạo phiên bản mới với "Từ ngày" mới.</span>
        <button className="btn sm" onClick={() => setEdit({ tourId: '', tenChinhSach: '', phienBan: 1, tuNgay: new Date().toISOString().slice(0, 10), denNgay: '', noiDung: '', mocPhats: [{ soNgayTruocTu: 0, soNgayTruocDen: 9999, phanTramPhat: 0 }] })}>+ Thêm chính sách</button>
      </div>
      {!rows ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Tên</th><th>Tour</th><th>Phiên bản</th><th>Hiệu lực</th><th>Mốc phạt</th><th></th></tr></thead>
          <tbody>{rows.map((c) => (
            <tr key={c.chinhSachHuyId}><td>{c.tenChinhSach}</td><td>{tours.find((t) => t.tourId === c.tourId)?.tenTour || 'Chung'}</td><td>v{c.phienBan}</td>
              <td className="small">{fmtDate(c.tuNgay)} → {c.denNgay ? fmtDate(c.denNgay) : 'nay'}</td>
              <td className="small">{c.mocPhats.map((m, i) => <div key={i}>{m.soNgayTruocTu}–{m.soNgayTruocDen >= 9999 ? '∞' : m.soNgayTruocDen} ngày: {m.phanTramPhat}%</div>)}</td>
              <td className="row"><button className="btn secondary sm" onClick={() => setEdit({ ...c, tourId: c.tourId || '', denNgay: c.denNgay || '', noiDung: c.noiDung || '' })}>Sửa</button>
                <button className="btn link small" onClick={async () => { if (confirm('Xóa chính sách?')) { try { await api.delete(`/api/admin/danh-muc/chinh-sach-huy/${c.chinhSachHuyId}`); load() } catch (e) { setErr(errMsg(e)) } } }}>Xóa</button></td></tr>
          ))}</tbody>
        </table></div>
      )}
      <Alert>{!edit ? err : ''}</Alert>
      {edit && (
        <Modal title="Chính sách hủy" onClose={() => setEdit(null)}>
          <div className="grid grid-2">
            <div className="field"><label>Tên *</label><input value={edit.tenChinhSach} onChange={(e) => setEdit({ ...edit, tenChinhSach: e.target.value })} /></div>
            <div className="field"><label>Áp dụng cho</label><select value={edit.tourId} onChange={(e) => setEdit({ ...edit, tourId: e.target.value })}><option value="">Chung mọi tour</option>{tours.map((t) => <option key={t.tourId} value={t.tourId}>{t.tenTour}</option>)}</select></div>
            <div className="field"><label>Phiên bản *</label><input type="number" min="1" value={edit.phienBan} onChange={(e) => setEdit({ ...edit, phienBan: e.target.value })} /></div>
            <div className="field"><label>Từ ngày *</label><input type="date" value={edit.tuNgay} onChange={(e) => setEdit({ ...edit, tuNgay: e.target.value })} /></div>
            <div className="field"><label>Đến ngày</label><input type="date" value={edit.denNgay} onChange={(e) => setEdit({ ...edit, denNgay: e.target.value })} /></div>
          </div>
          <div className="field"><label>Nội dung</label><textarea value={edit.noiDung} onChange={(e) => setEdit({ ...edit, noiDung: e.target.value })} /></div>
          <h4>Mốc phạt (số ngày hủy trước ngày khởi hành)</h4>
          {edit.mocPhats.map((m, i) => (
            <div key={i} className="row" style={{ marginBottom: 6 }}>
              Từ <input type="number" min="0" style={{ width: 90 }} value={m.soNgayTruocTu} onChange={(e) => setM(i, 'soNgayTruocTu', e.target.value)} />
              đến <input type="number" min="0" style={{ width: 90 }} value={m.soNgayTruocDen} onChange={(e) => setM(i, 'soNgayTruocDen', e.target.value)} />
              ngày → phạt <input type="number" min="0" max="100" style={{ width: 80 }} value={m.phanTramPhat} onChange={(e) => setM(i, 'phanTramPhat', e.target.value)} />%
              <button className="btn link" onClick={() => setEdit({ ...edit, mocPhats: edit.mocPhats.filter((_, j) => j !== i) })}>✕</button>
            </div>
          ))}
          <button className="btn secondary sm" onClick={() => setEdit({ ...edit, mocPhats: [...edit.mocPhats, { soNgayTruocTu: 0, soNgayTruocDen: 0, phanTramPhat: 100 }] })}>+ Mốc</button>
          <Alert>{err}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end', marginTop: 12 }}><button className="btn" onClick={save}>Lưu</button></div>
        </Modal>
      )}
    </>
  )
}
