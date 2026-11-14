import { useEffect, useState } from 'react'
import { Link, useOutletContext, useSearchParams } from 'react-router-dom'
import api, { errMsg } from '../../api'
import { phoneDigits } from '../../menus.js'
import { FAQ } from '../../content/trangChu.js'
import './contact.css'

/**
 * Trang Liên hệ = trung tâm tiếp nhận yêu cầu.
 * Form có cấu trúc theo loại yêu cầu; các trường bổ sung được ghép vào tiêu đề + nội dung
 * của bảng LienHe để quản trị viên đọc ngay trong mục "Liên hệ" (không cần đổi database).
 */

const LOAI = [
  { key: 'tu-van', ten: 'Tư vấn chọn tour', mo: 'Chưa biết chọn tour nào', tour: true, ngay: true, khach: true },
  { key: 'nhom', ten: 'Tour riêng cho nhóm', mo: 'Gia đình, cơ quan, trường học', tour: true, ngay: true, khach: true },
  { key: 'don', ten: 'Hỏi về đơn đã đặt', mo: 'Thanh toán, đổi, hủy', maDon: true },
  { key: 'khac', ten: 'Góp ý / hợp tác', mo: 'Nội dung khác' },
]
const KENH = ['Gọi điện', 'Zalo', 'Email']
const TODAY = new Date().toISOString().slice(0, 10)
const GIO = ['Bất kỳ lúc nào', 'Buổi sáng', 'Buổi chiều', 'Buổi tối']

const empty = { hoTen: '', soDienThoai: '', email: '', tourId: '', ngayDi: '', nguoiLon: '2', treEm: '0', maDon: '', kenh: 'Gọi điện', gio: 'Bất kỳ lúc nào', noiDung: '' }

/* Icon nét mảnh */
const IC = {
  phone: <path d="M5 4h3l2 5-2.5 1.5a11 11 0 0 0 6 6L15 14l5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2Z" />,
  chat: <path d="M4 5h16v11H9l-5 4V5Z" />,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  pin: <path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21Zm0-9a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z" />,
  check: <path d="m5 12 5 5 9-10" />,
}
const Icon = ({ name, className = 'ct-ic' }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{IC[name]}</svg>
)

export default function Contact() {
  const cfg = useOutletContext() || {}
  const [sp] = useSearchParams()
  const [loai, setLoai] = useState(sp.get('loai') && LOAI.some((l) => l.key === sp.get('loai')) ? sp.get('loai') : 'tu-van')
  const [f, setF] = useState({ ...empty, tourId: sp.get('tourId') || '' })
  const [tours, setTours] = useState([])
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [sent, setSent] = useState(null)

  useEffect(() => {
    api.get('/api/public/tours', { params: { size: 50 } }).then((r) => setTours(r.data.content)).catch(() => {})
  }, [])

  const L = LOAI.find((l) => l.key === loai)
  const tel = phoneDigits(cfg.HOTLINE)
  const bind = (k) => ({ value: f[k], onChange: (e) => setF({ ...f, [k]: e.target.value }) })
  const tenTour = tours.find((t) => String(t.tourId) === String(f.tourId))?.tenTour

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    // Ghép thông tin có cấu trúc vào tiêu đề + nội dung
    const thongTin = [
      `Loại yêu cầu: ${L.ten}`,
      L.tour && tenTour && `Tour quan tâm: ${tenTour}`,
      L.ngay && f.ngayDi && `Ngày đi dự kiến: ${new Date(f.ngayDi).toLocaleDateString('vi-VN')}`,
      L.khach && `Số khách: ${f.nguoiLon} người lớn, ${f.treEm} trẻ em`,
      L.maDon && f.maDon && `Mã đơn: ${f.maDon.trim()}`,
      `Liên hệ qua: ${f.kenh} (${f.gio.toLowerCase()})`,
    ].filter(Boolean)
    const dong = [...thongTin, '', f.noiDung.trim()]
    const tieuDe = [`[${L.ten}]`, L.tour && tenTour, L.maDon && f.maDon && `Đơn ${f.maDon.trim()}`].filter(Boolean).join(' ').slice(0, 250)
    try {
      await api.post('/api/public/lien-he', {
        hoTen: f.hoTen.trim(), email: f.email.trim(), soDienThoai: f.soDienThoai.trim(), tieuDe, noiDung: dong.join('\n').slice(0, 2000),
      })
      setSent({ ten: f.hoTen.trim(), kenh: f.kenh, loai: L.ten })
      setF({ ...empty })
      window.scrollTo({ top: document.getElementById('gui-yeu-cau')?.offsetTop - 90 || 0, behavior: 'smooth' })
    } catch (x) { setErr(errMsg(x)) } finally { setBusy(false) }
  }

  return (
    <div className="ct">
      {/* ---------- Mở đầu + kênh liên hệ nhanh ---------- */}
      <header className="ct-mast">
        <div className="container">
          <span className="eyebrow">Liên hệ</span>
          <h1>Chúng tôi luôn sẵn sàng lắng nghe</h1>
          <p className="ct-lead">Cần tư vấn hành trình, đặt tour cho nhóm hay hỏi về đơn đã đặt? Chọn cách thuận tiện nhất với bạn.</p>
          <div className="ct-quick">
            {tel && (
              <a className="ct-q primary" href={`tel:${tel}`}>
                <Icon name="phone" />
                <span><small>Gọi hotline</small><b>{cfg.HOTLINE}</b></span>
              </a>
            )}
            {tel && (
              <a className="ct-q" href={`https://zalo.me/${tel}`} target="_blank" rel="noreferrer">
                <Icon name="chat" />
                <span><small>Nhắn Zalo</small><b>Chat ngay</b></span>
              </a>
            )}
            {cfg.EMAIL && (
              <a className="ct-q" href={`mailto:${cfg.EMAIL}`}>
                <Icon name="mail" />
                <span><small>Gửi email</small><b>{cfg.EMAIL}</b></span>
              </a>
            )}
            <a className="ct-q" href="#gui-yeu-cau">
              <Icon name="check" />
              <span><small>Để lại yêu cầu</small><b>Chúng tôi gọi lại</b></span>
            </a>
          </div>
        </div>
      </header>

      <div className="container ct-main" id="gui-yeu-cau">
        {/* ---------- Form yêu cầu ---------- */}
        <section className="ct-form-card" aria-labelledby="ct-form-title">
          {sent ? (
            <div className="ct-done" role="status">
              <span className="ct-done-ic"><Icon name="check" className="" /></span>
              <h2>Cảm ơn {sent.ten || 'bạn'}, yêu cầu đã được gửi!</h2>
              <p>Chúng tôi đã nhận yêu cầu “{sent.loai}” và sẽ liên hệ lại qua <b>{sent.kenh.toLowerCase()}</b> trong giờ làm việc.</p>
              <ol className="ct-next">
                <li>Chuyên viên xem yêu cầu và chuẩn bị gợi ý phù hợp.</li>
                <li>Liên hệ để xác nhận thông tin, lịch trình và báo giá.</li>
                <li>Bạn đặt tour trực tuyến hoặc nhờ chúng tôi giữ chỗ.</li>
              </ol>
              <div className="ct-done-actions">
                <Link className="btn sun" to="/tour">Xem các tour</Link>
                <button type="button" className="btn secondary" onClick={() => setSent(null)}>Gửi yêu cầu khác</button>
              </div>
            </div>
          ) : (
            <form onSubmit={submit} noValidate={false}>
              <h2 id="ct-form-title">Gửi yêu cầu</h2>
              <p className="ct-sub">Điền vài thông tin, chuyên viên sẽ chuẩn bị sẵn gợi ý trước khi liên hệ.</p>

              <fieldset className="ct-types">
                <legend>Bạn cần hỗ trợ về</legend>
                {LOAI.map((l) => (
                  <label key={l.key} className={`ct-type ${loai === l.key ? 'on' : ''}`}>
                    <input type="radio" name="loai" value={l.key} checked={loai === l.key} onChange={() => setLoai(l.key)} />
                    <b>{l.ten}</b><small>{l.mo}</small>
                  </label>
                ))}
              </fieldset>

              <div className="ct-grid">
                <div className="field"><label htmlFor="ct-ten">Họ tên *</label><input id="ct-ten" required maxLength={150} autoComplete="name" {...bind('hoTen')} /></div>
                <div className="field"><label htmlFor="ct-sdt">Số điện thoại *</label><input id="ct-sdt" required type="tel" maxLength={20} pattern="[0-9 +().-]{8,20}" autoComplete="tel" {...bind('soDienThoai')} /></div>
                <div className="field span2"><label htmlFor="ct-email">Email *</label><input id="ct-email" required type="email" maxLength={150} autoComplete="email" {...bind('email')} /></div>

                {L.tour && (
                  <div className="field span2"><label htmlFor="ct-tour">Tour quan tâm</label>
                    <select id="ct-tour" {...bind('tourId')}>
                      <option value="">Chưa chọn – nhờ tư vấn giúp</option>
                      {tours.map((t) => <option key={t.tourId} value={t.tourId}>{t.tenTour}</option>)}
                    </select>
                  </div>
                )}
                {L.ngay && <div className="field"><label htmlFor="ct-ngay">Ngày đi dự kiến</label><input id="ct-ngay" type="date" min={TODAY} {...bind('ngayDi')} /></div>}
                {L.khach && (
                  <div className="field ct-guests">
                    <span className="ct-label">Số khách</span>
                    <label>Người lớn<input type="number" min="1" max="200" {...bind('nguoiLon')} /></label>
                    <label>Trẻ em<input type="number" min="0" max="200" {...bind('treEm')} /></label>
                  </div>
                )}
                {L.maDon && <div className="field span2"><label htmlFor="ct-don">Mã đơn</label><input id="ct-don" placeholder="VD: VL261112ABCDE" maxLength={30} {...bind('maDon')} /></div>}

                <div className="field"><label htmlFor="ct-kenh">Liên hệ lại qua</label>
                  <select id="ct-kenh" {...bind('kenh')}>{KENH.map((k) => <option key={k}>{k}</option>)}</select>
                </div>
                <div className="field"><label htmlFor="ct-gio">Thời gian thuận tiện</label>
                  <select id="ct-gio" {...bind('gio')}>{GIO.map((k) => <option key={k}>{k}</option>)}</select>
                </div>
                <div className="field span2"><label htmlFor="ct-nd">Nội dung *</label>
                  <textarea id="ct-nd" required maxLength={1500} rows={5}
                    placeholder={loai === 'don' ? 'Bạn cần hỗ trợ gì về đơn này?' : 'Ví dụ: nhóm có người lớn tuổi, muốn lịch trình nhẹ nhàng, có ăn chay…'} {...bind('noiDung')} />
                  <small className="ct-count">{f.noiDung.length}/1500</small>
                </div>
              </div>

              {err && <div className="alert error" role="alert">{err}</div>}
              <div className="ct-submit">
                <button className="btn sun" disabled={busy}>{busy ? 'Đang gửi…' : 'Gửi yêu cầu'}</button>
                <small>Thông tin chỉ dùng để liên hệ tư vấn, xem <Link to="/chinh-sach/bao-mat">Chính sách bảo mật</Link>.</small>
              </div>
            </form>
          )}
        </section>

        {/* ---------- Cột phụ (bento) ---------- */}
        <aside className="ct-side">
          <Link to="/tour" className="ct-tile book">
            <small>Đã chọn được tour?</small>
            <b>Đặt tour trực tuyến</b>
            <span>Chọn lịch, giữ chỗ và thanh toán qua VNPay →</span>
          </Link>
          <Link to="/tra-cuu" className="ct-tile">
            <small>Đã có đơn đặt tour?</small>
            <b>Tra cứu đơn</b>
            <span>Bằng mã đơn và số điện thoại →</span>
          </Link>
          <div className="ct-tile info">
            {cfg.DIA_CHI && <p><Icon name="pin" /><span><b>Văn phòng</b>{cfg.DIA_CHI}</span></p>}
            {cfg.GIO_LAM_VIEC && <p><Icon name="clock" /><span><b>Giờ làm việc</b>{cfg.GIO_LAM_VIEC}</span></p>}
            {cfg.BAN_DO_NHUNG
              ? <iframe title="Bản đồ văn phòng" src={cfg.BAN_DO_NHUNG} loading="lazy" referrerPolicy="no-referrer-when-downgrade" />
              : cfg.DIA_CHI && <a className="ct-map-link" href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(cfg.DIA_CHI)}`} target="_blank" rel="noreferrer">Xem trên Google Maps →</a>}
          </div>
        </aside>
      </div>

      {/* ---------- Hỏi nhanh ---------- */}
      <section className="container ct-faq">
        <h2>Có thể bạn đang tìm</h2>
        <div className="ct-faq-list">
          {FAQ.slice(0, 4).map(([q, a]) => (
            <details key={q}><summary>{q}</summary><p>{a}</p></details>
          ))}
        </div>
      </section>
    </div>
  )
}
