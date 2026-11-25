import { hhmm } from '../api'

const newAct = () => ({ startTime: '', title: '', description: '', destinationId: '' })
const newDay = () => ({ title: '', description: '', activities: [newAct()] })

/** ItineraryResponse.days từ API -> dạng form */
export const toEditable = (days = []) => days.map((n) => ({
  title: n.title || '', description: n.description || '',
  activities: (n.activities || []).map((h) => ({ startTime: hhmm(h.startTime), title: h.title || '', description: h.description || '', destinationId: h.destination?.id || '' })),
}))

/** Dạng form -> ItineraryRequest { days } (số ngày = thứ tự trong danh sách) */
export const toPayload = (days) => ({
  days: days.map((n, i) => ({
    title: n.title || `Ngày ${i + 1}`, description: n.description || null,
    activities: n.activities.filter((h) => h.title.trim()).map((h) => ({
      startTime: h.startTime || null, title: h.title, description: h.description || null, destinationId: h.destinationId ? Number(h.destinationId) : null,
    })),
  })),
})

/** Trình soạn lịch trình theo ngày / hoạt động; destinations: danh sách điểm đến { id, name } */
export default function ItineraryEditor({ value, onChange, destinations }) {
  const setDay = (i, patch) => onChange(value.map((d, j) => (j === i ? { ...d, ...patch } : d)))
  const setAct = (i, k, patch) => setDay(i, { activities: value[i].activities.map((a, j) => (j === k ? { ...a, ...patch } : a)) })
  const move = (i, k, dir) => {
    const arr = [...value[i].activities]; const t = k + dir
    if (t < 0 || t >= arr.length) return
    ;[arr[k], arr[t]] = [arr[t], arr[k]]
    setDay(i, { activities: arr })
  }
  return (
    <div className="stack">
      {value.map((d, i) => (
        <div key={i} className="box">
          <div className="row">
            <strong>Ngày {i + 1}</strong>
            <input className="grow" style={{ width: 'auto' }} placeholder="Tiêu đề ngày" value={d.title} onChange={(e) => setDay(i, { title: e.target.value })} />
            <button type="button" className="btn link small" onClick={() => onChange(value.filter((_, j) => j !== i))}>Xóa ngày</button>
          </div>
          <input style={{ marginTop: 6 }} placeholder="Mô tả ngày (không bắt buộc)" value={d.description} onChange={(e) => setDay(i, { description: e.target.value })} />
          <div style={{ marginTop: 10 }}>
            {d.activities.map((a, k) => (
              <div key={k} className="row" style={{ marginBottom: 6, alignItems: 'flex-start' }}>
                <input type="time" style={{ width: 110 }} value={a.startTime} onChange={(e) => setAct(i, k, { startTime: e.target.value })} aria-label="Giờ bắt đầu" />
                <input className="grow" style={{ width: 'auto', minWidth: 200 }} placeholder="Hoạt động" value={a.title} onChange={(e) => setAct(i, k, { title: e.target.value })} />
                <select style={{ width: 220 }} value={a.destinationId} onChange={(e) => setAct(i, k, { destinationId: e.target.value })}>
                  <option value="">— Không gắn điểm đến —</option>
                  {destinations.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
                </select>
                <button type="button" className="btn link small" onClick={() => move(i, k, -1)} title="Lên">↑</button>
                <button type="button" className="btn link small" onClick={() => move(i, k, 1)} title="Xuống">↓</button>
                <button type="button" className="btn link small" onClick={() => setDay(i, { activities: d.activities.filter((_, j) => j !== k) })}>✕</button>
              </div>
            ))}
            <button type="button" className="btn secondary sm" onClick={() => setDay(i, { activities: [...d.activities, newAct()] })}>+ Hoạt động</button>
          </div>
        </div>
      ))}
      <button type="button" className="btn secondary sm" onClick={() => onChange([...value, newDay()])}>+ Thêm ngày</button>
    </div>
  )
}
