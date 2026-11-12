import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { GoogleLogin } from '@react-oauth/google'
import { useAuth } from '../../auth.jsx'
import { errMsg } from '../../api'
import { Alert } from '../../components/ui.jsx'

export default function Login() {
  const { login, loginGoogle } = useAuth()
  const [sp] = useSearchParams()
  const nav = useNavigate()
  const [f, setF] = useState({ email: '', matKhau: '' })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const done = (u) => nav(sp.get('next') || (u.vaiTro === 'ADMIN' ? '/admin' : '/'))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try { done(await login(f.email, f.matKhau)) } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }

  return (
    <div className="container section" style={{ maxWidth: 440 }}>
      <form className="card" onSubmit={submit}>
        <h2>Đăng nhập</h2>
        <div className="field"><label>Email</label><input type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></div>
        <div className="field"><label>Mật khẩu</label><input type="password" required value={f.matKhau} onChange={(e) => setF({ ...f, matKhau: e.target.value })} /></div>
        <Alert>{err}</Alert>
        <button className="btn" style={{ width: '100%', marginTop: 8 }} disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
        <div className="muted small" style={{ textAlign: 'center', margin: '14px 0' }}>hoặc</div>
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          {import.meta.env.VITE_GOOGLE_CLIENT_ID ? (
            <GoogleLogin
              onSuccess={async (r) => { try { done(await loginGoogle(r.credential)) } catch (x) { setErr(errMsg(x)) } }}
              onError={() => setErr('Đăng nhập Google thất bại')}
              text="signin_with" locale="vi"
            />
          ) : <p className="muted small">Chưa cấu hình VITE_GOOGLE_CLIENT_ID để đăng nhập bằng Google.</p>}
        </div>
        <p className="small" style={{ textAlign: 'center', marginTop: 16 }}>Chưa có tài khoản? <Link to="/dang-ky">Đăng ký</Link></p>
      </form>
    </div>
  )
}
