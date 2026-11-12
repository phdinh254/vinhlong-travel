import { LOAI_HOAT_DONG } from '../api'

const newAct = () => ({ loaiHoatDong: 'THAM_QUAN', gioBatDau: '', gioKetThuc: '', noiDung: '', diemDenId: '' })

/** Chuyển dữ liệu NgayDto từ API -> dạng form */
export const toEditable = (ngays = []) => ngays.map((n) => ({
  ngayThuMay: n.ngayThuMay, tieuDeNgay: n.tieuDeNgay,
  hoatDongs: n.hoatDongs.map((h) => ({ loaiHoatDong: h.loaiHoatDong, gioBatDau: h.gioBatDau?.slice(0, 5) || '', gioKetThuc: h.gioKetThuc?.slice(0, 5) || '', noiDung: h.noiDung, diemDenId: h.diemDenId || '' })),
}))

/** Dạng form -> request NgayInput */
export const toPayload = (ngays) => ngays.map((n, i) => ({
  ngayThuMay: Number(n.ngayThuMay) || i + 1, tieuDeNgay: n.tieuDeNgay,
  hoatDongs: n.hoatDongs.map((h, j) => ({ loaiHoatDong: h.loaiHoatDong, gioBatDau: h.gioBatDau || null, gioKetThuc: h.gioKetThuc || null, noiDung: h.noiDung, thuTuHienThi: j + 1, diemDenId: h.diemDenId ? Number(h.diemDenId) : null })),
}))

/** Trình soạn lịch trình theo ngày / hoạt động (Q08–Q12) */
export default function ItineraryEditor({ value, onChange, diemDens }) {
  const setDay = (i, patch) => onChange(value.map((d, j) => (j === i ? { ...d, ...patch } : d)))
  const setAct = (i, k, patch) => setDay(i, { hoatDongs: value[i].hoatDongs.map((a, j) => (j === k ? { ...a, ...patch } : a)) })
  const move = (i, k, dir) => {
    const arr = [...value[i].hoatDongs]; const t = k + dir
    if (t < 0 || t >= arr.length) return
    ;[arr[k], arr[t]] = [arr[t], arr[k]]
    setDay(i, { hoatDongs: arr })
  }
  return (
    <div className="stack">
      {value.map((d, i) => (
        <div key={i} className="box">
          <div className="row">
            <strong>Ngày</strong>
            <input type="number" min="1" style={{ width: 70 }} value={d.ngayThuMay} onChange={(e) => setDay(i, { ngayThuMay: e.target.value })} />
            <input className="grow" style={{ width: 'auto' }} placeholder="Tiêu đề ngày" value={d.tieuDeNgay} onChange={(e) => setDay(i, { tieuDeNgay: e.target.value })} />
            <button type="button" className="btn link small" onClick={() => onChange(value.filter((_, j) => j !== i))}>Xóa ngày</button>
          </div>
          <div style={{ marginTop: 10 }}>
            {d.hoatDongs.map((a, k) => (
              <div key={k} className="row" style={{ marginBottom: 6, alignItems: 'flex-start' }}>
                <select style={{ width: 120 }} value={a.loaiHoatDong} onChange={(e) => setAct(i, k, { loaiHoatDong: e.target.value })}>
                  {Object.entries(LOAI_HOAT_DONG).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                </select>
                <input type="time" style={{ width: 110 }} value={a.gioBatDau} onChange={(e) => setAct(i, k, { gioBatDau: e.target.value })} aria-label="Giờ bắt đầu" />
                <input type="time" style={{ width: 110 }} value={a.gioKetThuc} onChange={(e) => setAct(i, k, { gioKetThuc: e.target.value })} aria-label="Giờ kết thúc" />
                <input className="grow" style={{ width: 'auto', minWidth: 200 }} placeholder="Nội dung hoạt động" value={a.noiDung} onChange={(e) => setAct(i, k, { noiDung: e.target.value })} />
                <select style={{ width: 200 }} value={a.diemDenId} onChange={(e) => setAct(i, k, { diemDenId: e.target.value })}>
                  <option value="">— Không gắn điểm đến —</option>
                  {diemDens.map((x) => <option key={x.diemDenId} value={x.diemDenId}>{x.tenDiemDen}</option>)}
                </select>
                <button type="button" className="btn link small" onClick={() => move(i, k, -1)} title="Lên">↑</button>
                <button type="button" className="btn link small" onClick={() => move(i, k, 1)} title="Xuống">↓</button>
                <button type="button" className="btn link small" onClick={() => setDay(i, { hoatDongs: d.hoatDongs.filter((_, j) => j !== k) })}>✕</button>
              </div>
            ))}
            <button type="button" className="btn secondary sm" onClick={() => setDay(i, { hoatDongs: [...d.hoatDongs, newAct()] })}>+ Hoạt động</button>
          </div>
        </div>
      ))}
      <button type="button" className="btn secondary sm" onClick={() => onChange([...value, { ngayThuMay: value.length + 1, tieuDeNgay: '', hoatDongs: [newAct()] }])}>+ Thêm ngày</button>
    </div>
  )
}
