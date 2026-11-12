import { useOutletContext } from 'react-router-dom'

export default function About() {
  const cfg = useOutletContext() || {}
  return (
    <>
      <div className="page-head"><div className="container"><h1>Giới thiệu</h1></div></div>
      <div className="container grid grid-2" style={{ gridTemplateColumns: '2fr 1fr' }}>
        {/* Nội dung do admin soạn trong mục "Giới thiệu & thông tin" */}
        <div className="card prose" dangerouslySetInnerHTML={{ __html: cfg.GIOI_THIEU || '<p>Đang cập nhật…</p>' }} />
        <div className="card">
          <h3>Thông tin liên hệ</h3>
          <p>📍 {cfg.DIA_CHI || '—'}</p>
          <p>📞 {cfg.HOTLINE || '—'}</p>
          <p>✉️ {cfg.EMAIL || '—'}</p>
        </div>
      </div>
    </>
  )
}
