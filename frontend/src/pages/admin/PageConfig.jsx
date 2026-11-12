import { useEffect, useState } from 'react'
import api, { errMsg } from '../../api'
import { Alert, Loading } from '../../components/ui.jsx'

const FIELDS = [
  ['GIOI_THIEU', 'Nội dung trang Giới thiệu (HTML)', true],
  ['HOTLINE', 'Hotline'],
  ['EMAIL', 'Email liên hệ'],
  ['DIA_CHI', 'Địa chỉ'],
  ['BAN_DO_NHUNG', 'Link nhúng bản đồ (Google Maps embed URL)'],
]

export default function PageConfig() {
  const [cfg, setCfg] = useState(null)
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  useEffect(() => { api.get('/api/admin/cau-hinh').then((r) => setCfg(r.data)) }, [])
  const save = async () => {
    setMsg(''); setErr('')
    try { setCfg((await api.put('/api/admin/cau-hinh', cfg)).data); setMsg('Đã lưu') } catch (e) { setErr(errMsg(e)) }
  }
  if (!cfg) return <Loading />
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
        <div className="box prose" dangerouslySetInnerHTML={{ __html: cfg.GIOI_THIEU || '' }} />
      </div>
    </>
  )
}
