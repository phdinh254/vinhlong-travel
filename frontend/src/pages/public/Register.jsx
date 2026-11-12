import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth.jsx'
import { errMsg } from '../../api'
import { Alert } from '../../components/ui.jsx'

export default function Register() {
  const { register } = useAuth()
  const nav = useNavigate()
  const [f, setF] = useState({ hoTen: '', email: '', soDienThoai: '', matKhau: '', nhapLai: '' })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const bind = (k) => ({ value: f[k], onChange: (e) => setF({ ...f, [k]: e.target.value }) })

  const submit = async (e) => {
    e.preventDefault()
    if (f.matKhau !== f.nhapLai) { setErr('Mật khẩu nhập lại không khớp'); return }
    setBusy(true); setErr('')
    try {
      const { nhapLai: _bo, ...body } = f
      await register(body)
      nav('/')
    } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }

  return (
    <div className="container section" style={{ maxWidth: 480 }}>
      <form className="card" onSubmit={submit}>
        <h2>Đăng ký tài khoản</h2>
        <div className="field"><label>Họ tên *</label><input required {...bind('hoTen')} /></div>
        <div className="field"><label>Email *</label><input type="email" required {...bind('email')} /></div>
        <div className="field"><label>Số điện thoại</label><input {...bind('soDienThoai')} /></div>
        <div className="grid grid-2">
          <div className="field"><label>Mật khẩu *</label><input type="password" minLength={6} required {...bind('matKhau')} /></div>
          <div className="field"><label>Nhập lại *</label><input type="password" required {...bind('nhapLai')} /></div>
        </div>
        <Alert>{err}</Alert>
        <button className="btn" style={{ width: '100%', marginTop: 8 }} disabled={busy}>Đăng ký</button>
        <p className="small" style={{ textAlign: 'center', marginTop: 16 }}>Đã có tài khoản? <Link to="/dang-nhap">Đăng nhập</Link> (hỗ trợ đăng nhập bằng Google)</p>
      </form>
    </div>
  )
}
