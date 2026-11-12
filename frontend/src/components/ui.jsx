import { useEffect, useState } from 'react'
import { imgUrl, money, uploadImage, errMsg } from '../api'
import { Link } from 'react-router-dom'

export function Badge({ map, value }) {
  const [label, cls] = map[value] || [value, 'muted']
  return <span className={`badge ${cls}`}>{label}</span>
}

export function Alert({ type = 'error', children }) {
  if (!children) return null
  return <div className={`alert ${type}`}>{children}</div>
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  return (
    <div className="pagination">
      <button className="btn secondary sm" disabled={page === 0} onClick={() => onChange(page - 1)}>‹ Trước</button>
      <span className="muted small" style={{ alignSelf: 'center' }}>Trang {page + 1}/{totalPages}</span>
      <button className="btn secondary sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Sau ›</button>
    </div>
  )
}

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    const h = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [onClose])
  return (
    <div className="modal-bg" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-label={title}>
        <div className="row between" style={{ marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>{title}</h3>
          <button className="btn link" onClick={onClose} aria-label="Đóng">✕</button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function TourCard({ t }) {
  const img = imgUrl(t.anhDaiDien)
  return (
    <div className="card flush tour-card">
      <Link to={`/tour/${t.tourId}`} className="thumb" style={img ? { backgroundImage: `url(${img})` } : undefined}>
        {t.noiBat && <span className="badge">Nổi bật</span>}
      </Link>
      <div className="body">
        <div className="chips">{t.khuVucs.map((k) => <span key={k} className="badge info">{k}</span>)}</div>
        <h3><Link to={`/tour/${t.tourId}`}>{t.tenTour}</Link></h3>
        <div className="muted small">⏱ {t.thoiLuong} · Mã {t.maTour}</div>
        <div className="grow" />
        <div className="row between">
          <div>
            <div className="muted small">Giá từ</div>
            <div className="price">{t.giaTu ? money(t.giaTu) : 'Liên hệ'}</div>
          </div>
          <Link className="btn sm" to={`/tour/${t.tourId}`}>Xem tour</Link>
        </div>
      </div>
    </div>
  )
}

/** Ô chọn ảnh: upload lên server, trả về đường dẫn */
export function ImagePicker({ value, onChange, label = 'Ảnh' }) {
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const pick = async (e) => {
    const f = e.target.files?.[0]
    if (!f) return
    setBusy(true); setErr('')
    try { onChange(await uploadImage(f)) } catch (x) { setErr(errMsg(x)) } finally { setBusy(false); e.target.value = '' }
  }
  return (
    <div className="field">
      <label>{label}</label>
      <div className="row">
        <input value={value || ''} onChange={(e) => onChange(e.target.value)} placeholder="Đường dẫn ảnh hoặc tải lên" className="grow" style={{ width: 'auto' }} />
        <label className="btn secondary sm" style={{ margin: 0 }}>
          {busy ? 'Đang tải…' : 'Tải ảnh'}
          <input type="file" accept="image/*" hidden onChange={pick} />
        </label>
      </div>
      {value && <img src={imgUrl(value)} alt="" style={{ maxHeight: 120, marginTop: 8, borderRadius: 8 }} />}
      {err && <div className="alert error small" style={{ marginTop: 6 }}>{err}</div>}
    </div>
  )
}

export function Loading() {
  return <p className="muted">Đang tải…</p>
}
