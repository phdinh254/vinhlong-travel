import { LOAI_HOAT_DONG } from '../api'

/** Hiển thị lịch trình theo ngày (Q08–Q12) */
export default function Itinerary({ ngays }) {
  if (!ngays?.length) return <p className="muted">Chưa có lịch trình chi tiết.</p>
  return (
    <div className="timeline">
      {ngays.map((n) => (
        <div className="day" key={n.ngayLichTrinhId ?? n.ngayThuMay}>
          <h4>Ngày {n.ngayThuMay}: {n.tieuDeNgay}</h4>
          {n.hoatDongs.map((h, i) => (
            <div className="act" key={h.hoatDongId ?? i}>
              <div className="muted small">
                {h.gioBatDau ? h.gioBatDau.slice(0, 5) : ''}{h.gioKetThuc ? ` – ${h.gioKetThuc.slice(0, 5)}` : ''}
                <br /><span className="badge muted">{LOAI_HOAT_DONG[h.loaiHoatDong]}</span>
              </div>
              <div>
                {h.noiDung}
                {h.tenDiemDen && <div className="small" style={{ color: 'var(--green)' }}>📍 {h.tenDiemDen}</div>}
              </div>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}
