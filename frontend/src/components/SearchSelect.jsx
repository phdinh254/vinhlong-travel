import { useEffect, useId, useRef, useState } from 'react'

/** Bộ icon nét mảnh (24×24, stroke = currentColor) dùng cho thanh tìm kiếm */
const PATHS = {
  pin: <><path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21Z" /><circle cx="12" cy="9.5" r="2.5" /></>,
  map: <><path d="m9 4-6 2.5v13.5l6-2.5 6 2.5 6-2.5V4l-6 2.5L9 4Z" /><path d="M9 4v13.5M15 6.5V20" /></>,
  compass: <><circle cx="12" cy="12" r="9" /><path d="m15.5 8.5-2 5-5 2 2-5 5-2Z" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></>,
  arrow: <path d="M5 12h14m-6-6 6 6-6 6" />,
  chevron: <path d="m6 9 6 6 6-6" />,
  check: <path d="m5 12 5 5 9-10" />,
}
export function Icon({ name, size = 18, className }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">{PATHS[name]}</svg>
  )
}

/**
 * Ô chọn tùy biến (thay <select>) có animation mở/đóng.
 * Bàn phím: ↑/↓ mở & di chuyển, Home/End, Enter/Space chọn, Esc/Tab đóng, gõ chữ để nhảy tới mục.
 * options: [{ value, label }] — value '' là lựa chọn mặc định (placeholder).
 */
export default function SearchSelect({ label, icon, value, onChange, options }) {
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const root = useRef(null)
  const list = useRef(null)
  const typed = useRef({ s: '', t: 0 })
  const id = useId()
  const selIdx = Math.max(0, options.findIndex((o) => String(o.value) === String(value)))
  const current = options[selIdx]

  // Đóng khi bấm ra ngoài
  useEffect(() => {
    if (!open) return
    const off = (e) => { if (!root.current?.contains(e.target)) setOpen(false) }
    document.addEventListener('pointerdown', off)
    return () => document.removeEventListener('pointerdown', off)
  }, [open])

  // Giữ mục đang trỏ trong vùng nhìn thấy của danh sách
  useEffect(() => {
    if (open) list.current?.children[active]?.scrollIntoView({ block: 'nearest' })
  }, [open, active])

  const show = () => { setActive(selIdx); setOpen(true) }
  const pick = (i) => { onChange(String(options[i].value)); setOpen(false) }

  const onKey = (e) => {
    const last = options.length - 1
    const keys = {
      ArrowDown: () => (open ? setActive((a) => Math.min(last, a + 1)) : show()),
      ArrowUp: () => (open ? setActive((a) => Math.max(0, a - 1)) : show()),
      Home: () => open && setActive(0),
      End: () => open && setActive(last),
      Enter: () => (open ? pick(active) : show()),
      ' ': () => (open ? pick(active) : show()),
      Escape: () => setOpen(false),
    }
    if (keys[e.key]) { e.preventDefault(); keys[e.key](); return }
    if (e.key === 'Tab') { setOpen(false); return }
    if (e.key.length === 1 && /\S/.test(e.key)) {
      // Gõ chữ để nhảy tới mục bắt đầu bằng chuỗi đó
      const now = Date.now(); const t = typed.current
      t.s = (now - t.t > 700 ? '' : t.s) + e.key.toLowerCase(); t.t = now
      const i = options.findIndex((o) => o.label.toLowerCase().startsWith(t.s))
      if (i >= 0) { if (!open) setOpen(true); setActive(i) }
    }
  }

  return (
    <div className={`ss${open ? ' open' : ''}${String(value) ? ' has-value' : ''}`} ref={root}>
      <span className="ss-label" id={`${id}-l`}><Icon name={icon} size={15} />{label}</span>
      <button type="button" className="ss-btn" aria-haspopup="listbox" aria-expanded={open} aria-controls={`${id}-list`}
        aria-labelledby={`${id}-l ${id}-v`} aria-activedescendant={open ? `${id}-${active}` : undefined}
        onClick={() => (open ? setOpen(false) : show())} onKeyDown={onKey}>
        <span className="ss-value" id={`${id}-v`}>{current?.label}</span>
        <Icon name="chevron" size={18} className="ss-chev" />
      </button>
      <ul className="ss-list" role="listbox" id={`${id}-list`} aria-labelledby={`${id}-l`} ref={list} tabIndex={-1}>
        {options.map((o, i) => (
          <li key={String(o.value)} id={`${id}-${i}`} role="option" aria-selected={i === selIdx}
            className={`${i === active ? 'active' : ''}${i === selIdx ? ' sel' : ''}`}
            onPointerMove={() => setActive(i)} onPointerDown={(e) => e.preventDefault()} onClick={() => pick(i)}>
            <span>{o.label}</span>{i === selIdx && <Icon name="check" size={16} />}
          </li>
        ))}
      </ul>
    </div>
  )
}
