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

/** Đăng ký: cùng bố cục chia đôi với trang đăng nhập */
export default function Register() {
  const { register, loginGoogle } = useAuth()
  const [sp] = useSearchParams()
  const nav = useNavigate()
  const [f, setF] = useState({ fullName: '', email: '', phone: '', password: '', nhapLai: '' })
  const [show, setShow] = useState(false)
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const next = sp.get('next')
  const loginLink = `/dang-nhap${next ? `?next=${encodeURIComponent(next)}` : ''}`
  const bind = (k) => ({ value: f[k], onChange: (e) => setF({ ...f, [k]: e.target.value }) })

  const done = (u) => nav(next || (u?.role === 'ADMIN' ? '/admin' : '/'))

  const submit = async (e) => {
    e.preventDefault()
    if (f.password !== f.nhapLai) { setErr('Mật khẩu nhập lại không khớp'); return }
    setBusy(true); setErr('')
    try {
      const { nhapLai: _bo, ...body } = f
      if (!body.phone) delete body.phone
      done(await register(body))
    } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }

  return (
    <section className="lg-page">
      <div className="lg-card">
        <div className="lg-visual">
          <img src="/images/kenh-rach.jpg" alt="Thuyền trên kênh rạch miền Tây" />
          <div className="lg-visual-in">
            <h2>Bắt đầu hành trình<br />miền Tây của bạn.</h2>
            <Link className="lg-signup" to={loginLink}>
              Đăng nhập
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M10 8.5 13.5 12 10 15.5M8 12h5.5" strokeLinecap="round" strokeLinejoin="round" /></svg>
            </Link>
          </div>
          <a className="lg-credit" href="https://commons.wikimedia.org/wiki/File:Canal_in_Mekong_Delta_-_My_Tho_-_Vietnam_(15912635245).jpg" target="_blank" rel="noreferrer">
            Ảnh: Esin Üstün · CC BY 2.0
          </a>
        </div>

        <form className="lg-form lg-form--reg" onSubmit={submit}>
          <h1>Tạo tài khoản<br />để đặt tour dễ hơn.</h1>

          <label className="lg-field">
            <span className="sr-only">Họ tên</span>
            <input required autoComplete="name" placeholder="Họ và tên" {...bind('fullName')} />
          </label>

          <label className="lg-field">
            <span className="sr-only">Email</span>
            <input type="email" required autoComplete="email" placeholder="Email" {...bind('email')} />
          </label>

          <label className="lg-field">
            <span className="sr-only">Số điện thoại</span>
            <input type="tel" autoComplete="tel" placeholder="Số điện thoại (tùy chọn)" {...bind('phone')} />
          </label>

          <label className="lg-field">
            <span className="sr-only">Mật khẩu</span>
            <input type={show ? 'text' : 'password'} required minLength={8} autoComplete="new-password" placeholder="Mật khẩu (từ 8 ký tự)" {...bind('password')} />
            <button type="button" className="lg-eye" onClick={() => setShow(!show)} aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>
              <Eye off={!show} />
            </button>
          </label>

          <label className="lg-field">
            <span className="sr-only">Nhập lại mật khẩu</span>
            <input type={show ? 'text' : 'password'} required autoComplete="new-password" placeholder="Nhập lại mật khẩu" {...bind('nhapLai')} />
          </label>

          {err && <p className="lg-err" role="alert">{err}</p>}

          <button className="lg-submit" disabled={busy}>{busy ? 'Đang tạo tài khoản…' : 'Đăng ký'}</button>

          {import.meta.env.VITE_GOOGLE_CLIENT_ID && (
            <>
              <div className="lg-or"><span>hoặc đăng ký nhanh với</span></div>
              <div className="lg-social">
                <GoogleLogin
                  type="icon" shape="circle" size="large" locale="vi"
                  onSuccess={async (r) => { try { done(await loginGoogle(r.credential)) } catch (x) { setErr(errMsg(x)) } }}
                  onError={() => setErr('Đăng nhập Google thất bại')}
                />
              </div>
            </>
          )}

          <p className="lg-foot">Đã có tài khoản? <Link to={loginLink}>Đăng nhập</Link></p>
        </form>
      </div>
    </section>
  )
}
