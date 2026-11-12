import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import api, { errMsg, imgUrl, uploadImage, money, fmtDateTime, toLocalInput, TRANG_THAI_DOT } from '../../api'
import { Alert, Badge, Loading, Modal } from '../../components/ui.jsx'
import ItineraryEditor, { toEditable, toPayload } from '../../components/ItineraryEditor.jsx'

const emptyTour = { maTour: '', tenTour: '', moTa: '', thoiLuong: '', soNgay: 1, trangThai: 'NHAP', noiBat: false,
  loaiHinhIds: [], khuVucIds: [], hinhAnhs: [], dichVus: [], lichTrinh: [] }

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

  useEffect(() => {
    const keys = ['loai-hinh', 'khu-vuc', 'dich-vu', 'diem-den', 'loai-khach', 'diem-don']
    Promise.all(keys.map((k) => api.get(`/api/admin/danh-muc/${k}`))).then((rs) => {
      const [loaiHinh, khuVuc, dichVu, diemDen, loaiKhach, diemDon] = rs.map((r) => r.data)
      setDm({ loaiHinh, khuVuc, dichVu, diemDen, loaiKhach, diemDon })
    })
  }, [])

  const loadTour = () => api.get(`/api/admin/tours/${id}`).then((r) => {
    const t = r.data
    setF({ maTour: t.maTour, tenTour: t.tenTour, moTa: t.moTa || '', thoiLuong: t.thoiLuong, soNgay: t.soNgay, trangThai: t.trangThai, noiBat: t.noiBat,
      loaiHinhIds: t.loaiHinhs.map((x) => x.id), khuVucIds: t.khuVucs.map((x) => x.id),
      hinhAnhs: t.hinhAnhs.map((h) => ({ duongDan: h.duongDan, laAnhDaiDien: h.laAnhDaiDien })),
      dichVus: t.dichVus.map((d) => ({ dichVuId: d.dichVuId, loaiApDung: d.loaiApDung, ghiChu: d.ghiChu || '' })),
      lichTrinh: toEditable(t.lichTrinh) })
    setDots(t.dots)
  }).catch((e) => setErr(errMsg(e)))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { if (id) loadTour() }, [id])

  const toggle = (k, v) => setF({ ...f, [k]: f[k].includes(v) ? f[k].filter((x) => x !== v) : [...f[k], v] })

  const save = async () => {
    setBusy(true); setErr(''); setMsg('')
    try {
      const body = { ...f, soNgay: Number(f.soNgay), hinhAnhs: f.hinhAnhs.map((h, i) => ({ ...h, thuTuHienThi: i })), lichTrinh: toPayload(f.lichTrinh) }
      const { data } = id ? await api.put(`/api/admin/tours/${id}`, body) : await api.post('/api/admin/tours', body)
      setMsg('Đã lưu tour')
      if (!id) nav(`/admin/tours/${data.tourId}`, { replace: true })
    } catch (e) { setErr(errMsg(e)) } finally { setBusy(false) }
  }

  const addImages = async (e) => {
    const files = [...e.target.files]
    try {
      const urls = await Promise.all(files.map(uploadImage))
      setF((p) => ({ ...p, hinhAnhs: [...p.hinhAnhs, ...urls.map((u) => ({ duongDan: u, laAnhDaiDien: false }))] }))
    } catch (x) { setErr(errMsg(x)) }
    e.target.value = ''
  }

  const setDichVu = (dichVuId, loaiApDung) => {
    const rest = f.dichVus.filter((d) => d.dichVuId !== dichVuId)
    setF({ ...f, dichVus: loaiApDung ? [...rest, { dichVuId, loaiApDung, ghiChu: f.dichVus.find((d) => d.dichVuId === dichVuId)?.ghiChu || '' }] : rest })
  }

  const delDot = async (d) => {
    if (!confirm(`Xóa đợt ${d.maDot}?`)) return
    try { await api.delete(`/api/admin/dots/${d.dotKhoiHanhId}`); loadTour() } catch (e) { setErr(errMsg(e)) }
  }

  if (!dm || (id && !f.maTour && !err)) return <Loading />

  return (
    <>
      <div className="admin-head">
        <h1>{id ? `Sửa tour ${f.maTour}` : 'Thêm tour'}</h1>
        <div className="row"><Link className="btn secondary" to="/admin/tours">← Danh sách</Link><button className="btn" disabled={busy} onClick={save}>{busy ? 'Đang lưu…' : 'Lưu tour'}</button></div>
      </div>
      <Alert>{err}</Alert><Alert type="ok">{msg}</Alert>
      <div className="tabs">
        {[['info', 'Thông tin'], ['img', `Hình ảnh (${f.hinhAnhs.length})`], ['dv', 'Dịch vụ'], ['lt', 'Lịch trình mẫu'], ...(id ? [['dot', `Đợt khởi hành (${dots.length})`]] : [])]
          .map(([k, l]) => <button key={k} className={tab === k ? 'active' : ''} onClick={() => setTab(k)}>{l}</button>)}
      </div>

      {tab === 'info' && (
        <div className="card">
          <div className="grid grid-4">
            <div className="field"><label>Mã tour *</label><input value={f.maTour} onChange={(e) => setF({ ...f, maTour: e.target.value })} /></div>
            <div className="field" style={{ gridColumn: 'span 3' }}><label>Tên tour *</label><input value={f.tenTour} onChange={(e) => setF({ ...f, tenTour: e.target.value })} /></div>
            <div className="field"><label>Thời lượng *</label><input placeholder="2 ngày 1 đêm" value={f.thoiLuong} onChange={(e) => setF({ ...f, thoiLuong: e.target.value })} /></div>
            <div className="field"><label>Số ngày *</label><input type="number" min="1" value={f.soNgay} onChange={(e) => setF({ ...f, soNgay: e.target.value })} /></div>
            <div className="field"><label>Trạng thái</label><select value={f.trangThai} onChange={(e) => setF({ ...f, trangThai: e.target.value })}><option value="NHAP">Nháp</option><option value="DANG_BAN">Đang bán</option><option value="NGUNG_BAN">Ngừng bán</option></select></div>
            <div className="field"><label>&nbsp;</label><label className="check"><input type="checkbox" checked={f.noiBat} onChange={(e) => setF({ ...f, noiBat: e.target.checked })} /> Tour nổi bật</label></div>
          </div>
          <div className="field"><label>Mô tả (cho phép HTML)</label><textarea style={{ minHeight: 160 }} value={f.moTa} onChange={(e) => setF({ ...f, moTa: e.target.value })} /></div>
          <div className="grid grid-2">
            <div className="field"><label>Loại hình du lịch (Q02)</label><div className="box">{dm.loaiHinh.map((x) => <label key={x.loaiHinhId} className="check"><input type="checkbox" checked={f.loaiHinhIds.includes(x.loaiHinhId)} onChange={() => toggle('loaiHinhIds', x.loaiHinhId)} />{x.tenLoaiHinh}</label>)}</div></div>
            <div className="field"><label>Khu vực (Q03)</label><div className="box">{dm.khuVuc.map((x) => <label key={x.khuVucId} className="check"><input type="checkbox" checked={f.khuVucIds.includes(x.khuVucId)} onChange={() => toggle('khuVucIds', x.khuVucId)} />{x.tenKhuVuc}</label>)}</div></div>
          </div>
        </div>
      )}

      {tab === 'img' && (
        <div className="card">
          <p className="muted small">Ảnh có viền vàng là ảnh đại diện. Thứ tự hiển thị theo thứ tự bên dưới.</p>
          <div className="thumbs">
            {f.hinhAnhs.map((h, i) => (
              <div key={i} className={`t ${h.laAnhDaiDien ? 'main' : ''}`}>
                <div style={{ backgroundImage: `url(${imgUrl(h.duongDan)})` }} />
                <div className="row small" style={{ gap: 4, marginTop: 4 }}>
                  <button className="btn link small" onClick={() => setF({ ...f, hinhAnhs: f.hinhAnhs.map((x, j) => ({ ...x, laAnhDaiDien: j === i })) })}>Đại diện</button>
                  <button className="btn link small" disabled={i === 0} onClick={() => { const a = [...f.hinhAnhs]; [a[i - 1], a[i]] = [a[i], a[i - 1]]; setF({ ...f, hinhAnhs: a }) }}>←</button>
                  <button className="btn link small" onClick={() => setF({ ...f, hinhAnhs: f.hinhAnhs.filter((_, j) => j !== i) })}>Xóa</button>
                </div>
              </div>
            ))}
          </div>
          <label className="btn secondary" style={{ marginTop: 14 }}>+ Tải ảnh lên<input type="file" accept="image/*" multiple hidden onChange={addImages} /></label>
        </div>
      )}

      {tab === 'dv' && (
        <div className="card">
          <div className="table-wrap"><table>
            <thead><tr><th>Dịch vụ</th><th>Áp dụng (Q07)</th><th>Ghi chú</th></tr></thead>
            <tbody>{dm.dichVu.map((d) => {
              const cur = f.dichVus.find((x) => x.dichVuId === d.dichVuId)
              return (
                <tr key={d.dichVuId}><td>{d.tenDichVu}</td>
                  <td><select value={cur?.loaiApDung || ''} onChange={(e) => setDichVu(d.dichVuId, e.target.value)}>
                    <option value="">— Không đề cập —</option><option value="BAO_GOM">Bao gồm</option><option value="KHONG_BAO_GOM">Không bao gồm</option></select></td>
                  <td><input disabled={!cur} value={cur?.ghiChu || ''} onChange={(e) => setF({ ...f, dichVus: f.dichVus.map((x) => (x.dichVuId === d.dichVuId ? { ...x, ghiChu: e.target.value } : x)) })} /></td></tr>
              )
            })}</tbody>
          </table></div>
        </div>
      )}

      {tab === 'lt' && (
        <div className="card">
          <p className="muted small">Lịch trình mẫu của tour. Khi tạo đợt mới có thể sao chép lịch trình này sang đợt để chỉnh riêng (Q13).</p>
          <ItineraryEditor value={f.lichTrinh} onChange={(v) => setF({ ...f, lichTrinh: v })} diemDens={dm.diemDen} />
        </div>
      )}

      {tab === 'dot' && id && (
        <div className="card">
          <div className="row between" style={{ marginBottom: 12 }}><h3 style={{ margin: 0 }}>Đợt khởi hành</h3><button className="btn sm" onClick={() => setDotEdit({})}>+ Thêm đợt</button></div>
          <div className="table-wrap"><table>
            <thead><tr><th>Mã đợt</th><th>Khởi hành</th><th>Về</th><th>Chỗ</th><th>Bảng giá</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>{dots.map((d) => (
              <tr key={d.dotKhoiHanhId}>
                <td>{d.maDot}</td><td>{fmtDateTime(d.ngayGioDi)}<div className="muted small">Hạn nhận: {fmtDateTime(d.hanNhanKhach)}</div></td><td>{fmtDateTime(d.ngayGioVe)}</td>
                <td>{d.soChoDaDat}/{d.sucChuaToiDa}</td>
                <td className="small">{d.bangGia.map((g) => <div key={g.loaiKhachId}>{g.tenLoaiKhach}: {money(g.donGia)}</div>)}</td>
                <td><Badge map={TRANG_THAI_DOT} value={d.trangThai} />{d.coLichTrinhRieng && <div className="small muted">Lịch trình riêng</div>}</td>
                <td>
                  <button className="btn secondary sm" onClick={() => setDotEdit(d)}>Sửa</button>{' '}
                  <button className="btn link small" onClick={() => setDotLT(d)}>Lịch trình</button>{' '}
                  <Link className="btn link small" to={`/admin/don-dat?dotId=${d.dotKhoiHanhId}`}>Đơn</Link>{' '}
                  <button className="btn link small" onClick={() => delDot(d)}>Xóa</button>
                </td>
              </tr>
            ))}</tbody>
          </table></div>
        </div>
      )}

      {dotEdit && <DotForm tourId={Number(id)} maTour={f.maTour} dot={dotEdit} dm={dm} onClose={() => setDotEdit(null)} onSaved={() => { setDotEdit(null); loadTour() }} />}
      {dotLT && <DotItinerary dot={dotLT} dm={dm} onClose={() => setDotLT(null)} onSaved={() => { setDotLT(null); loadTour() }} />}
    </>
  )
}

/** Form đợt khởi hành + bảng giá theo loại khách + điểm đón (Q14–Q20) */
function DotForm({ tourId, maTour, dot, dm, onClose, onSaved }) {
  const isNew = !dot.dotKhoiHanhId
  const [f, setF] = useState({
    maDot: dot.maDot || `${maTour}-`, ngayGioDi: toLocalInput(dot.ngayGioDi), ngayGioVe: toLocalInput(dot.ngayGioVe),
    sucChuaToiDa: dot.sucChuaToiDa || 30, hanNhanKhach: toLocalInput(dot.hanNhanKhach), trangThai: dot.trangThai || 'MO_BAN',
    saoChepLichTrinh: true,
  })
  const [gia, setGia] = useState(() => Object.fromEntries(dm.loaiKhach.map((l) => [l.loaiKhachId, dot.bangGia?.find((g) => g.loaiKhachId === l.loaiKhachId)?.donGia ?? ''])))
  const [dds, setDds] = useState(dot.diemDons?.map((d) => ({ diemDonId: d.diemDonId, gioDon: toLocalInput(d.gioDon), phiPhuThuDon: d.phiPhuThuDon })) || [])
  const [err, setErr] = useState('')

  const save = async () => {
    setErr('')
    try {
      const body = { tourId, ...f, sucChuaToiDa: Number(f.sucChuaToiDa),
        bangGia: Object.entries(gia).filter(([, v]) => v !== '' && v != null).map(([k, v]) => ({ loaiKhachId: Number(k), donGia: Number(v) })),
        diemDons: dds.filter((d) => d.diemDonId).map((d) => ({ diemDonId: Number(d.diemDonId), gioDon: d.gioDon, phiPhuThuDon: Number(d.phiPhuThuDon) || 0 })) }
      if (isNew) await api.post('/api/admin/dots', body); else await api.put(`/api/admin/dots/${dot.dotKhoiHanhId}`, body)
      onSaved()
    } catch (e) { setErr(errMsg(e)) }
  }

  return (
    <Modal title={isNew ? 'Thêm đợt khởi hành' : `Sửa đợt ${dot.maDot}`} onClose={onClose}>
      <div className="grid grid-3">
        <div className="field"><label>Mã đợt *</label><input value={f.maDot} onChange={(e) => setF({ ...f, maDot: e.target.value })} /></div>
        <div className="field"><label>Ngày giờ đi *</label><input type="datetime-local" value={f.ngayGioDi} onChange={(e) => setF({ ...f, ngayGioDi: e.target.value })} /></div>
        <div className="field"><label>Ngày giờ về *</label><input type="datetime-local" value={f.ngayGioVe} onChange={(e) => setF({ ...f, ngayGioVe: e.target.value })} /></div>
        <div className="field"><label>Sức chứa tối đa *</label><input type="number" min="1" value={f.sucChuaToiDa} onChange={(e) => setF({ ...f, sucChuaToiDa: e.target.value })} /></div>
        <div className="field"><label>Hạn nhận khách *</label><input type="datetime-local" value={f.hanNhanKhach} onChange={(e) => setF({ ...f, hanNhanKhach: e.target.value })} /></div>
        <div className="field"><label>Trạng thái</label><select value={f.trangThai} onChange={(e) => setF({ ...f, trangThai: e.target.value })}>{Object.entries(TRANG_THAI_DOT).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}</select></div>
      </div>
      <h4>Bảng giá theo loại khách (Q16–Q17)</h4>
      <div className="grid grid-3">
        {dm.loaiKhach.map((l) => (
          <div key={l.loaiKhachId} className="field"><label>{l.tenLoaiKhach} ({l.tuoiTu}–{l.tuoiDen} tuổi)</label>
            <input type="number" min="0" step="1000" value={gia[l.loaiKhachId]} onChange={(e) => setGia({ ...gia, [l.loaiKhachId]: e.target.value })} placeholder="Để trống = không bán" /></div>
        ))}
      </div>
      <h4>Điểm đón (Q20)</h4>
      {dds.map((d, i) => (
        <div key={i} className="row" style={{ marginBottom: 6 }}>
          <select className="grow" style={{ width: 'auto' }} value={d.diemDonId} onChange={(e) => setDds(dds.map((x, j) => (j === i ? { ...x, diemDonId: e.target.value } : x)))}>
            <option value="">— Chọn điểm đón —</option>{dm.diemDon.map((x) => <option key={x.diemDonId} value={x.diemDonId}>{x.tenDiemDon}</option>)}
          </select>
          <input type="datetime-local" style={{ width: 210 }} value={d.gioDon} onChange={(e) => setDds(dds.map((x, j) => (j === i ? { ...x, gioDon: e.target.value } : x)))} />
          <input type="number" min="0" step="1000" style={{ width: 140 }} placeholder="Phụ thu/khách" value={d.phiPhuThuDon} onChange={(e) => setDds(dds.map((x, j) => (j === i ? { ...x, phiPhuThuDon: e.target.value } : x)))} />
          <button className="btn link" onClick={() => setDds(dds.filter((_, j) => j !== i))}>✕</button>
        </div>
      ))}
      <button className="btn secondary sm" onClick={() => setDds([...dds, { diemDonId: '', gioDon: f.ngayGioDi, phiPhuThuDon: 0 }])}>+ Điểm đón</button>
      {isNew && <label className="check" style={{ marginTop: 14 }}><input type="checkbox" checked={f.saoChepLichTrinh} onChange={(e) => setF({ ...f, saoChepLichTrinh: e.target.checked })} /> Sao chép lịch trình mẫu sang đợt này (Q13)</label>}
      <Alert>{err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 14 }}><button className="btn secondary" onClick={onClose}>Hủy</button><button className="btn" onClick={save}>Lưu đợt</button></div>
    </Modal>
  )
}

/** Lịch trình riêng của đợt (Q13). Lưu danh sách rỗng = dùng lịch trình mẫu của tour */
function DotItinerary({ dot, dm, onClose, onSaved }) {
  const [v, setV] = useState(null)
  const [err, setErr] = useState('')
  useEffect(() => { api.get(`/api/admin/dots/${dot.dotKhoiHanhId}/lich-trinh`).then((r) => setV(toEditable(r.data))) }, [dot])
  const save = async (data) => {
    try { await api.put(`/api/admin/dots/${dot.dotKhoiHanhId}/lich-trinh`, data); onSaved() } catch (e) { setErr(errMsg(e)) }
  }
  return (
    <Modal title={`Lịch trình đợt ${dot.maDot}`} onClose={onClose}>
      <p className="muted small">{dot.coLichTrinhRieng ? 'Đợt đang dùng lịch trình riêng.' : 'Đợt đang dùng lịch trình mẫu của tour; lưu để tạo phiên bản riêng cho đợt.'}</p>
      {!v ? <Loading /> : <ItineraryEditor value={v} onChange={setV} diemDens={dm.diemDen} />}
      <Alert>{err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 14 }}>
        {dot.coLichTrinhRieng && <button className="btn secondary" onClick={() => save([])}>Quay về lịch trình mẫu</button>}
        <button className="btn" onClick={() => save(toPayload(v))}>Lưu lịch trình đợt</button>
      </div>
    </Modal>
  )
}
