import { useEffect, useState } from 'react'
import api, { errMsg } from '../../api'
import { Alert, Loading } from '../../components/ui.jsx'

const FIELDS = [
  ['ABOUT_HTML', 'Lời giới thiệu đơn vị (HTML, hiện ở khung cuối trang Giới thiệu)', true],
  ['HOTLINE', 'Hotline'],
  ['EMAIL', 'Email liên hệ'],
  ['ADDRESS', 'Địa chỉ'],
  ['MAP_EMBED_URL', 'Link nhúng bản đồ (Google Maps embed URL)'],
  ['SUPPORT_HOURS', 'Giờ hỗ trợ hiển thị ở footer (VD: Hỗ trợ 7:00 – 21:00)'],
  ['FACEBOOK_URL', 'Link Facebook'],
  ['INSTAGRAM_URL', 'Link Instagram'],
  ['YOUTUBE_URL', 'Link YouTube'],
  ['TIKTOK_URL', 'Link TikTok'],
  ['TERMS_HTML', 'Điều khoản đặt tour (HTML)', true],
  ['PRIVACY_HTML', 'Chính sách bảo mật (HTML)', true],
  ['CANCELLATION_HTML', 'Chính sách hủy tour (HTML)', true],
]

export default function PageConfig() {
  const [cfg, setCfg] = useState(null)
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  useEffect(() => { api.get('/api/v1/site-settings').then((r) => setCfg(r.data)).catch((e) => setErr(errMsg(e))) }, [])
  const save = async () => {
    setMsg(''); setErr('')
    try { setCfg((await api.put('/api/v1/admin/site-settings', cfg)).data); setMsg('Đã lưu') } catch (e) { setErr(errMsg(e)) }
  }
  if (!cfg) return err ? <Alert>{err}</Alert> : <Loading />
  return (
    <>
      <div className="admin-head"><h1>Giới thiệu & thông tin liên hệ</h1><button className="btn" onClick={save}>Lưu</button></div>
      <Alert type="ok">{msg}</Alert><Alert>{err}</Alert>
      <div className="card">
        {FIELDS.map(([k, label, big]) => (
          <div key={k} className="field"><label>{label}</label>
            {big ? <textarea style={{ minHeight: 260 }} value={cfg[k] || ''} onChange={(e) => setCfg({ ...cfg, [k]: e.target.value })} />
              : <input value={cfg[k] || ''} onChange={(e) => setCfg({ ...cfg, [k]: e.target.value })} />}
          </div>
        ))}
        <h4>Xem trước Giới thiệu</h4>
        <div className="box prose" dangerouslySetInnerHTML={{ __html: cfg.ABOUT_HTML || '' }} />
      </div>
    </>
  )
}
