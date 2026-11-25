import { useCallback, useEffect, useRef, useState } from 'react'

const AUTO_MS = 5000
const reduceMotion = () => typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

/**
 * Trình chiếu ảnh: một ảnh lớn, chuyển bằng nút ‹ ›, phím mũi tên, vuốt trên điện thoại hoặc bấm ảnh nhỏ bên dưới.
 * Tự chuyển ảnh sau mỗi 5 giây; dừng khi rê chuột / đang thao tác và khi người dùng bật "giảm chuyển động".
 * images: [{ src, caption }]
 */
export default function ImageCarousel({ images, label = 'Ảnh' }) {
  const [i, setI] = useState(0)
  const [paused, setPaused] = useState(false)
  const touchX = useRef(null)
  const thumbs = useRef(null)
  const n = images.length

  const go = useCallback((k) => setI(((k % n) + n) % n), [n])
  const prev = () => go(i - 1)
  const next = useCallback(() => go(i + 1), [go, i])

  // Tự chuyển ảnh
  useEffect(() => {
    if (n < 2 || paused || reduceMotion()) return
    const t = setTimeout(next, AUTO_MS)
    return () => clearTimeout(t)
  }, [i, n, paused, next])

  // Giữ ảnh nhỏ đang chọn ở giữa dải ảnh (chỉ cuộn ngang dải ảnh, không kéo cả trang)
  useEffect(() => {
    const box = thumbs.current
    const el = box?.children[i]
    if (!el) return
    box.scrollTo({ left: el.offsetLeft - (box.clientWidth - el.clientWidth) / 2, behavior: reduceMotion() ? 'auto' : 'smooth' })
  }, [i])

  if (n === 0) return <div className="ic-stage ic-empty" aria-label="Chưa có ảnh" />

  const onKey = (e) => {
    if (e.key === 'ArrowLeft') { e.preventDefault(); prev() }
    if (e.key === 'ArrowRight') { e.preventDefault(); next() }
  }
  const onTouchEnd = (e) => {
    if (touchX.current == null) return
    const dx = e.changedTouches[0].clientX - touchX.current
    if (Math.abs(dx) > 40) (dx > 0 ? prev : next)()
    touchX.current = null
  }

  return (
    <section className="ic" aria-roledescription="carousel" aria-label={label}
      onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)} onBlurCapture={() => setPaused(false)}>
      <div className="ic-stage" tabIndex={0} onKeyDown={onKey}
        onTouchStart={(e) => { touchX.current = e.touches[0].clientX }} onTouchEnd={onTouchEnd}>
        <div className="ic-track" style={{ transform: `translateX(-${i * 100}%)` }}>
          {images.map((im, k) => (
            <figure key={k} className="ic-slide" aria-roledescription="slide" aria-label={`${k + 1} / ${n}`} aria-hidden={k !== i}>
              <img src={im.src} alt={im.caption || `${label} ${k + 1}`} loading={k === 0 ? 'eager' : 'lazy'} draggable="false" />
            </figure>
          ))}
        </div>
        {n > 1 && (
          <>
            <button type="button" className="ic-nav prev" onClick={prev} aria-label="Ảnh trước">‹</button>
            <button type="button" className="ic-nav next" onClick={next} aria-label="Ảnh sau">›</button>
            <span className="ic-count" aria-live="polite">{i + 1} / {n}</span>
          </>
        )}
        {images[i].caption && <p className="ic-caption">{images[i].caption}</p>}
      </div>
      {n > 1 && (
        <div className="ic-thumbs" ref={thumbs} role="tablist" aria-label="Chọn ảnh">
          {images.map((im, k) => (
            <button key={k} type="button" role="tab" aria-selected={k === i} aria-label={`Xem ảnh ${k + 1}`}
              className={`ic-thumb ${k === i ? 'on' : ''}`} onClick={() => go(k)}>
              <img src={im.src} alt="" loading="lazy" />
            </button>
          ))}
        </div>
      )}
    </section>
  )
}
