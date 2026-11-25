import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { money, loadCatalogs, EMPTY_CATALOGS } from '../../api'
import { useReveal } from '../../useReveal.js'
import './services.css'

/**
 * Trang Dịch vụ: các tiện ích và hỗ trợ ĐI KÈM / BỔ TRỢ cho tour (không bán sản phẩm riêng lẻ).
 * Dữ liệu thật lấy từ API danh mục (dịch vụ trong tour, phụ thu tùy chọn).
 */

const img = (k) => `/images/${k}.jpg`

/* Icon nét mảnh */
const IC = {
  bus: <><rect x="4" y="4" width="16" height="13" rx="2" /><path d="M4 11h16M8 20v-3M16 20v-3" /></>,
  guide: <><circle cx="12" cy="7" r="3" /><path d="M6 21v-2a6 6 0 0 1 12 0v2" /></>,
  home: <><path d="M3 11 12 4l9 7" /><path d="M5 10v10h14V10M10 20v-5h4v5" /></>,
  meal: <><path d="M3 11h18a9 9 0 0 1-18 0Z" /><path d="M8 7c0-2 2-2 2-4M13 7c0-2 2-2 2-4" /></>,
  shield: <path d="M12 3 5 6v6c0 4.4 3 7.6 7 9 4-1.4 7-4.6 7-9V6l-7-3Z" />,
  ticket: <><path d="M4 7h16v3a2 2 0 0 0 0 4v3H4v-3a2 2 0 0 0 0-4V7Z" /><path d="M14 7v10" strokeDasharray="2 2" /></>,
  chat: <path d="M4 5h16v11H9l-5 4V5Z" />,
  card: <><rect x="3" y="6" width="18" height="12" rx="2" /><path d="M3 10h18M7 15h3" /></>,
  search: <><circle cx="11" cy="11" r="6" /><path d="m20 20-4-4" /></>,
  refresh: <><path d="M4 12a8 8 0 0 1 14-5.3L20 9" /><path d="M20 4v5h-5M20 12a8 8 0 0 1-14 5.3L4 15" /><path d="M4 20v-5h5" /></>,
  users: <><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6M14.5 14.2c3.2-.6 6.5 1.6 6.5 5.8" /></>,
}
const Icon = ({ name }) => (
  <svg className="sv-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{IC[name]}</svg>
)

/** Dịch vụ đi kèm tour (bento). `dv` = tên dịch vụ trong DB để tạo link lọc "tour có bao gồm" */
const DI_KEM = [
  { key: 'xe', size: 'xl', ic: 'bus', dv: 'Xe đưa đón', more: 'Tour có xe đưa đón', ten: 'Xe đưa đón tận nơi',
    mo: 'Xe đời mới đón khách tại các điểm đón cố định theo từng đợt khởi hành. Bạn chọn điểm đón ngay khi đặt tour.' },
  { key: 'hdv', size: 'tall', ic: 'guide', dv: 'Hướng dẫn viên', more: 'Tour có hướng dẫn viên', ten: 'Hướng dẫn viên địa phương', image: 'homestay',
    mo: 'Người con của miền sông nước, thuyết minh văn hóa Kinh – Khmer – Hoa và lo chu toàn suốt hành trình.' },
  { key: 'luu-tru', size: 'md', ic: 'home', dv: 'Khách sạn', more: 'Tour có lưu trú', ten: 'Lưu trú homestay', image: 'an-binh',
    mo: 'Nghỉ đêm tại nhà vườn trên cù lao với các tour nhiều ngày.' },
  { key: 'an', size: 'sm', ic: 'meal', dv: 'Bữa ăn', more: 'Tour có bữa ăn', ten: 'Bữa ăn món quê', mo: 'Thực đơn theo chương trình, đặc sản từng vùng.' },
  { key: 've', size: 'sm', ic: 'ticket', dv: 'Vé tham quan', more: 'Tour đã gồm vé', ten: 'Vé tham quan', mo: 'Đã gồm vé vào các điểm trong lịch trình.' },
  { key: 'bh', size: 'wide', ic: 'shield', dv: 'Bảo hiểm', more: 'Tour có bảo hiểm', ten: 'Bảo hiểm du lịch',
    mo: 'Mỗi hành khách được bảo hiểm trong suốt chuyến đi; có thể nâng cấp gói bảo hiểm khi đặt tour.' },
]

/** Hỗ trợ trước, trong và sau chuyến đi */
const HO_TRO = [
  { ic: 'chat', ten: 'Tư vấn chọn tour', mo: 'Gợi ý hành trình theo số ngày, ngân sách và thành phần đoàn.', to: '/lien-he', cta: 'Gửi yêu cầu' },
  { ic: 'card', ten: 'Đặt & thanh toán trực tuyến', mo: 'Giữ chỗ bằng tiền cọc hoặc thanh toán đủ qua VNPay.', to: '/tour', cta: 'Chọn tour' },
  { ic: 'search', ten: 'Tra cứu đơn mọi lúc', mo: 'Xem trạng thái đơn bằng mã đơn và số điện thoại, không cần tài khoản.', to: '/tra-cuu', cta: 'Tra cứu đơn' },
  { ic: 'refresh', ten: 'Hủy tour minh bạch', mo: 'Gửi yêu cầu hủy trực tuyến, phí hủy theo mốc ngày được công bố trước.', to: '/chinh-sach/huy-tour', cta: 'Xem chính sách' },
  { ic: 'users', ten: 'Tour riêng cho nhóm', mo: 'Điều chỉnh lịch trình các tour có sẵn cho gia đình, cơ quan, trường học.', to: '/lien-he', cta: 'Yêu cầu báo giá' },
]

const DON_VI = { PER_PERSON: '/ người', PER_BOOKING: '/ đơn', PER_UNIT: '/ đơn vị' }

export default function Services() {
  const { search } = useLocation()
  const [dm, setDm] = useState(EMPTY_CATALOGS)
  const root = useReveal()

  useEffect(() => {
    loadCatalogs().then(setDm)
  }, [])

  // Link cũ /dich-vu?khuVucId=... (trước đây là danh sách tour) chuyển sang trang Tour du lịch
  if (search) return <Navigate to={`/tour${search}`} replace />

  const tourCo = (ten) => {
    const d = dm.amenities.find((x) => x.name.toLowerCase().includes(ten.toLowerCase()))
    return d ? `/tour?amenityId=${d.id}` : '/tour'
  }

  return (
    <div className="sv" ref={root}>
      {/* ---------- Masthead ---------- */}
      <header className="sv-mast">
        <svg className="sv-leaf" viewBox="0 0 200 200" aria-hidden="true"><path d="M100 10C150 40 190 90 170 140S90 200 50 170 10 80 40 50 70 0 100 10Z" /></svg>
        <div className="container">
          <span className="eyebrow">Dịch vụ</span>
          <h1>Mọi thứ cho một chuyến đi trọn vẹn</h1>
          <p className="sv-lead">Những tiện ích và hỗ trợ đi kèm mỗi tour của chúng tôi, để bạn chỉ việc chọn hành trình và tận hưởng miền Tây.</p>
          <div className="sv-mast-cta">
            <Link className="btn sun" to="/tour">Chọn tour</Link>
            <a className="sv-link" href="#tuy-chon">Xem tùy chọn thêm ↓</a>
          </div>
        </div>
      </header>

      {/* ---------- Đi kèm trong tour (bento) ---------- */}
      <section className="container sv-sec">
        <div className="sv-head is-center">
          <span className="eyebrow">Đi kèm trong tour</span>
          <h2>Đã lo sẵn cho bạn</h2>
          <p>Mức bao gồm khác nhau theo từng tour, xem mục “Bao gồm” trong trang chi tiết hoặc lọc tour theo dịch vụ.</p>
        </div>
        <div className="sv-bento">
          {DI_KEM.map((d) => (
            <article key={d.key} className={`sv-b ${d.size} ${d.image ? 'photo' : ''}`} data-reveal
              style={d.image ? { backgroundImage: `url(${img(d.image)})` } : undefined}>
              <div className="sv-b-body">
                <Icon name={d.ic} />
                <h3>{d.ten}</h3>
                <p>{d.mo}</p>
                <Link className="sv-more" to={tourCo(d.dv)}>{d.more} →</Link>
              </div>
            </article>
          ))}
        </div>
      </section>

      {/* ---------- Tùy chọn thêm khi đặt tour ---------- */}
      {dm.surcharges.length > 0 && (
        <section className="sv-band" id="tuy-chon">
          <div className="container sv-sec">
            <div className="sv-head is-center">
              <span className="eyebrow">Tùy chọn thêm</span>
              <h2>Chọn thêm khi đặt tour</h2>
              <p>Bạn chọn các tiện ích này ở bước đặt tour; tổng tiền được tính và hiển thị trước khi xác nhận.</p>
            </div>
            <ul className="sv-addons">
              {dm.surcharges.map((p) => (
                <li key={p.id} data-reveal>
                  <span className="sv-addon-name">{p.name}</span>
                  <span className="sv-addon-price">
                    {p.unitPrice > 0 ? <><b>{money(p.unitPrice)}</b> <small>{DON_VI[p.method] || ''}</small></> : <b>Miễn phí</b>}
                  </span>
                </li>
              ))}
            </ul>
            <p className="sv-note">* Một số tour có thể có thêm phụ thu riêng (ví dụ điểm đón xa), được ghi rõ trong trang chi tiết tour.</p>
          </div>
        </section>
      )}

      {/* ---------- Hỗ trợ trước & sau chuyến đi ---------- */}
      <section className="container sv-sec">
        <div className="sv-head is-center">
          <span className="eyebrow">Hỗ trợ khách hàng</span>
          <h2>Đồng hành trước, trong và sau chuyến đi</h2>
        </div>
        <ol className="sv-support">
          {HO_TRO.map((h, i) => (
            <li key={h.ten} data-reveal style={{ '--d': `${i * 70}ms` }}>
              <Icon name={h.ic} />
              <div>
                <h3>{h.ten}</h3>
                <p>{h.mo}</p>
              </div>
              <Link to={h.to} className="sv-support-cta">{h.cta} →</Link>
            </li>
          ))}
        </ol>
      </section>

      {/* ---------- CTA ---------- */}
      <section className="container sv-sec">
        <div className="sv-cta" data-reveal>
          <div>
            <h2>Sẵn sàng lên đường?</h2>
            <p>Chọn một hành trình có sẵn, hoặc để chúng tôi gợi ý tour phù hợp với bạn.</p>
          </div>
          <div className="sv-cta-actions">
            <Link className="btn sun" to="/tour">Xem tất cả tour</Link>
            <Link className="btn ghost" to="/lien-he">Nhờ tư vấn</Link>
          </div>
        </div>
      </section>
    </div>
  )
}
