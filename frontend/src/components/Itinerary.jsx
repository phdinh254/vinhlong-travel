import { hhmm } from '../api'

/** Hiển thị lịch trình theo ngày: days = [{ dayNumber, title, description, activities: [{ startTime, title, description, destination }] }] */
export default function Itinerary({ days }) {
  if (!days?.length) return <p className="muted">Chưa có lịch trình chi tiết.</p>
  return (
    <div className="timeline">
      {days.map((n) => (
        <div className="day" key={n.dayNumber}>
          <h4>Ngày {n.dayNumber}: {n.title}</h4>
          {n.description && <p className="small muted" style={{ margin: '0 0 6px' }}>{n.description}</p>}
          {(n.activities || []).map((h, i) => (
            <div className="act" key={i}>
              <div className="muted small">{hhmm(h.startTime)}</div>
              <div>
                {h.title}
                {h.description && <div className="small muted">{h.description}</div>}
                {h.destination && <div className="small" style={{ color: 'var(--green)' }}>📍 {h.destination.name}</div>}
              </div>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}
