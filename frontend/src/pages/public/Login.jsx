import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { GoogleLogin } from '@react-oauth/google'
import { useAuth } from '../../auth.jsx'
import { errMsg } from '../../api'
import './login.css'

const Eye = ({ off }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
    <circle cx="12" cy="12" r="3" />
    {off && <path d="M4 20 20 4" />}
  </svg>
)

/** Đăng nhập: bố cục chia đôi – ảnh bên trái, biểu mẫu bên phải */
export default function Login() {
  const { login, loginGoogle } = useAuth()
  const [sp] = useSearchParams()
  const nav = useNavigate()
  const [f, setF] = useState({ email: '', matKhau: '' })
  const [show, setShow] = useState(false)
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const next = sp.get('next')

  const done = (u) => nav(next || (u.vaiTro === 'ADMIN' ? '/admin' : '/'))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try { done(await login(f.email, f.matKhau)) } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }

  return (
    <section className="lg-page">
      <div className="lg-card">
        <div className="lg-visual">
          <img src="/images/vui-choi.jpg" alt="Du khách vui chơi bên Ao Bà Om, Trà Vinh" />
          <div className="lg-visual-in">
            <h2>Tìm chuyến đi<br />tiếp theo của bạn.</h2>
            <Link className="lg-signup" to={`/dang-ky${next ? `?next=${encodeURIComponent(next)}` : ''}`}>
              Đăng ký
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M10 8.5 13.5 12 10 15.5M8 12h5.5" strokeLinecap="round" strokeLinejoin="round" /></svg>
            </Link>
          </div>
          <a className="lg-credit" href="https://commons.wikimedia.org/wiki/File:Vui_ch%C6%A1i_%E1%BB%9F_ao_B%C3%A0_%C3%94m.jpg" target="_blank" rel="noreferrer">
            Ảnh: Bùi Thụy Đào Nguyên · CC BY-SA 3.0
          </a>
        </div>

        <form className="lg-form" onSubmit={submit} noValidate={false}>
          <h1>Chào bạn, rất vui<br />được gặp lại.</h1>

          <label className="lg-field">
            <span className="sr-only">Email</span>
            <input type="email" required autoComplete="email" placeholder="Email"
              value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />
          </label>

          <label className="lg-field">
            <span className="sr-only">Mật khẩu</span>
            <input type={show ? 'text' : 'password'} required autoComplete="current-password" placeholder="Mật khẩu"
              value={f.matKhau} onChange={(e) => setF({ ...f, matKhau: e.target.value })} />
            <button type="button" className="lg-eye" onClick={() => setShow(!show)} aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>
              <Eye off={!show} />
            </button>
          </label>

          {err && <p className="lg-err" role="alert">{err}</p>}

          <button className="lg-submit" disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>

          <div className="lg-or"><span>hoặc đăng nhập với</span></div>

          <div className="lg-social">
            {import.meta.env.VITE_GOOGLE_CLIENT_ID ? (
              <GoogleLogin
                type="icon" shape="circle" size="large" locale="vi"
                onSuccess={async (r) => { try { done(await loginGoogle(r.credential)) } catch (x) { setErr(errMsg(x)) } }}
                onError={() => setErr('Đăng nhập Google thất bại')}
              />
            ) : <small>Chưa cấu hình VITE_GOOGLE_CLIENT_ID</small>}
          </div>

          <p className="lg-foot">Chưa có tài khoản? <Link to={`/dang-ky${next ? `?next=${encodeURIComponent(next)}` : ''}`}>Đăng ký ngay</Link></p>
        </form>
      </div>
    </section>
  )
}
