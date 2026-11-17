import { Link, useOutletContext } from 'react-router-dom'
import { HERO, FACTS, OVERVIEW, HISTORY, CULTURE, PEOPLE, NATURE, VALUES } from '../../content/gioiThieu.js'
import './about.css'

const img = (k) => `/images/${k}.jpg`

/* Icon nét mảnh cho mục "Giá trị đặc trưng" */
const VALUE_ICON = {
  river: <><path d="M3 8c3 0 3-2 6-2s3 2 6 2 3-2 6-2" /><path d="M3 13c3 0 3-2 6-2s3 2 6 2 3-2 6-2" /><path d="M3 18c3 0 3-2 6-2s3 2 6 2 3-2 6-2" /></>,
  palm: <><path d="M12 21V10" /><path d="M12 10C10 6 6 5 3 6c3 1 6 2 9 4Z" /><path d="M12 10c2-4 6-5 9-4-3 1-6 2-9 4Z" /><path d="M12 10c-1-3-1-5 0-7 1 2 1 4 0 7Z" /></>,
  temple: <><path d="M12 3 8 8h8l-4-5Z" /><path d="M6 8h12l-1 4H7L6 8Z" /><path d="M7 12v8M17 12v8M4 20h16M12 15v5" /></>,
  kiln: <><path d="M5 20V11a7 7 0 0 1 14 0v9" /><path d="M3 20h18M9 20v-5a3 3 0 0 1 6 0v5" /></>,
  bowl: <><path d="M3 11h18a9 9 0 0 1-18 0Z" /><path d="M8 7c0-2 2-2 2-4M13 7c0-2 2-2 2-4" /></>,
  flag: <><path d="M5 21V4" /><path d="M5 4h11l-2 4 2 4H5" /></>,
}

const Figure = ({ k, caption, className = '' }) => (
  <figure className={`ab-fig ${className}`}>
    <img src={img(k)} alt={caption} loading="lazy" />
    <figcaption>{caption}</figcaption>
  </figure>
)

const SectionHead = ({ eyebrow, title, center = false, children }) => (
  <div className={`ab-head${center ? ' is-center' : ''}`}>
    <span className="eyebrow">{eyebrow}</span>
    <h2>{title}</h2>
    {children && <p>{children}</p>}
  </div>
)

export default function About() {
  const cfg = useOutletContext() || {}
  return (
    <div className="ab">
      {/* ---------- Mở đầu ---------- */}
      <section className="ab-hero" style={{ backgroundImage: `url(${img(HERO.image)})` }}>
        <div className="container ab-hero-in">
          <span className="eyebrow light">{HERO.eyebrow}</span>
          <h1>{HERO.title}</h1>
          <p>{HERO.lead}</p>
        </div>
      </section>

      {/* ---------- Số liệu nhanh ---------- */}
      <div className="container">
        <ul className="ab-facts">
          {FACTS.map(([n, label, note]) => (
            <li key={label}><b>{n}</b><strong>{label}</strong><span>{note}</span></li>
          ))}
        </ul>
      </div>

      {/* ---------- Tổng quan ---------- */}
      <section className="container ab-sec ab-split">
        <div>
          <SectionHead eyebrow="Vùng đất" title={OVERVIEW.title} />
          {OVERVIEW.paragraphs.map((p) => <p key={p.slice(0, 20)} className="ab-text">{p}</p>)}
        </div>
        <Figure k={OVERVIEW.image} caption={OVERVIEW.caption} className="tall" />
      </section>

      {/* ---------- Lịch sử ---------- */}
      <section className="ab-band">
        <div className="container ab-sec">
          <SectionHead eyebrow="Dòng chảy thời gian" title="Lịch sử" center>
            Gần ba thế kỷ hình thành và phát triển của vùng đất phía nam sông Tiền.
          </SectionHead>
          <ol className="ab-timeline">
            {HISTORY.map(([year, title, text]) => (
              <li key={year}>
                <span className="ab-year">{year}</span>
                <h3>{title}</h3>
                <p>{text}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* ---------- Văn hóa ---------- */}
      <section className="container ab-sec">
        <SectionHead eyebrow="Bản sắc" title="Văn hóa" center />
        <div className="ab-cards">
          {CULTURE.map((c) => (
            <article key={c.title} className="ab-card">
              <Figure k={c.image} caption={c.caption} />
              <div className="ab-card-body">
                <h3>{c.title}</h3>
                <p>{c.text}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      {/* ---------- Con người ---------- */}
      <section className="container ab-sec ab-split reverse">
        <div className="ab-duo">
          {PEOPLE.images.map(([k, cap]) => <Figure key={k} k={k} caption={cap} />)}
        </div>
        <div>
          <SectionHead eyebrow="Hồn quê" title={PEOPLE.title} />
          <blockquote className="ab-quote">{PEOPLE.quote}</blockquote>
          {PEOPLE.paragraphs.map((p) => <p key={p.slice(0, 20)} className="ab-text">{p}</p>)}
        </div>
      </section>

      {/* ---------- Thiên nhiên ---------- */}
      <section className="ab-band">
        <div className="container ab-sec">
          <SectionHead eyebrow="Cảnh sắc" title="Thiên nhiên" center />
          <div className="ab-nature">
            {NATURE.map((n) => (
              <article key={n.title} className="ab-tile" style={{ backgroundImage: `url(${img(n.image)})` }}>
                <div>
                  <h3>{n.title}</h3>
                  <p>{n.text}</p>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* ---------- Giá trị đặc trưng ---------- */}
      <section className="container ab-sec">
        <SectionHead eyebrow="Điều làm nên Vĩnh Long" title="Giá trị đặc trưng" center />
        <ul className="ab-values">
          {VALUES.map(([icon, title, text]) => (
            <li key={title}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{VALUE_ICON[icon]}</svg>
              <h3>{title}</h3>
              <p>{text}</p>
            </li>
          ))}
        </ul>
      </section>

      {/* ---------- Về đơn vị + CTA ---------- */}
      <section className="container ab-sec">
        <div className="ab-cta">
          <div>
            <h2>Sẵn sàng khám phá Vĩnh Long?</h2>
            {cfg.GIOI_THIEU
              ? <div className="ab-org" dangerouslySetInnerHTML={{ __html: cfg.GIOI_THIEU }} />
              : <p>Chọn một hành trình để tự mình trải nghiệm sông nước, xứ dừa và văn hóa Khmer.</p>}
          </div>
          <div className="ab-cta-actions">
            <Link className="btn sun" to="/tour">Xem các tour</Link>
            <Link className="btn ghost" to="/lien-he">Nhờ tư vấn</Link>
          </div>
        </div>
      </section>
    </div>
  )
}
