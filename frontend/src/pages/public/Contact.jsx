import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import api, { errMsg } from '../../api'
import { Alert } from '../../components/ui.jsx'

const empty = { hoTen: '', email: '', soDienThoai: '', tieuDe: '', noiDung: '' }

export default function Contact() {
  const cfg = useOutletContext() || {}
  const [f, setF] = useState(empty)
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr(''); setMsg('')
    try { setMsg((await api.post('/api/public/lien-he', f)).data.message); setF(empty) }
    catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }
  const bind = (k) => ({ value: f[k], onChange: (e) => setF({ ...f, [k]: e.target.value }) })

  return (
    <>
      <div className="page-head"><div className="container"><h1>Liên hệ</h1></div></div>
      <div className="container grid" style={{ gridTemplateColumns: '1fr 2fr' }}>
        <div className="card">
          <h3>Thông tin</h3>
          <p>📍 {cfg.DIA_CHI || '—'}</p><p>📞 {cfg.HOTLINE || '—'}</p><p>✉️ {cfg.EMAIL || '—'}</p>
          {cfg.BAN_DO_NHUNG && <iframe title="Bản đồ" src={cfg.BAN_DO_NHUNG} style={{ width: '100%', height: 240, border: 0, borderRadius: 8 }} loading="lazy" />}
        </div>
        <form className="card" onSubmit={submit}>
          <h3>Gửi lời nhắn</h3>
          <div className="grid grid-3">
            <div className="field"><label>Họ tên *</label><input required {...bind('hoTen')} /></div>
            <div className="field"><label>Email *</label><input required type="email" {...bind('email')} /></div>
            <div className="field"><label>Số điện thoại</label><input {...bind('soDienThoai')} /></div>
          </div>
          <div className="field"><label>Tiêu đề</label><input {...bind('tieuDe')} /></div>
          <div className="field"><label>Nội dung *</label><textarea required maxLength={2000} {...bind('noiDung')} /></div>
          <Alert type="ok">{msg}</Alert><Alert>{err}</Alert>
          <button className="btn" disabled={busy} style={{ marginTop: 10 }}>{busy ? 'Đang gửi…' : 'Gửi liên hệ'}</button>
        </form>
      </div>
    </>
  )
}
