import { Navigate, useOutletContext, useParams } from 'react-router-dom'
import { POLICY_MENU } from '../../menus.js'

/** Trang chính sách: nội dung HTML do admin soạn, lưu theo khóa cấu hình trong POLICY_MENU */
export default function Policy() {
  const { slug } = useParams()
  const cfg = useOutletContext() || {}
  const item = POLICY_MENU.find(([s]) => s === slug)
  if (!item) return <Navigate to="/" replace />
  const [, title, key] = item
  return (
    <>
      <div className="page-head"><div className="container"><h1>{title}</h1></div></div>
      <div className="container">
        <div className="card prose" dangerouslySetInnerHTML={{ __html: cfg[key] || '<p>Nội dung đang được cập nhật.</p>' }} />
      </div>
    </>
  )
}
