import { useEffect, useRef } from 'react'

/**
 * Scroll reveal: phần tử có thuộc tính data-reveal bên trong vùng ref sẽ nhận class "in"
 * khi cuộn tới. Tôn trọng prefers-reduced-motion (hiện ngay, không hiệu ứng).
 */
export function useReveal() {
  const ref = useRef(null)
  useEffect(() => {
    const root = ref.current
    if (!root) return
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    const show = (el) => el.classList.add('in')
    const scan = () => root.querySelectorAll('[data-reveal]:not(.in)')
    if (reduce || !('IntersectionObserver' in window)) { scan().forEach(show); return }
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => { if (e.isIntersecting) { show(e.target); io.unobserve(e.target) } })
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 })
    const observeAll = () => scan().forEach((el) => io.observe(el))
    observeAll()
    // Nội dung tải sau (tour, tin tức) cũng được theo dõi
    const mo = new MutationObserver(observeAll)
    mo.observe(root, { childList: true, subtree: true })
    return () => { io.disconnect(); mo.disconnect() }
  }, [])
  return ref
}
