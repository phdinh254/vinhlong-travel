import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import api, { errMsg, fmtDate, imgUrl } from '../../api'
import { Alert, Loading } from '../../components/ui.jsx'

export default function NewsDetail() {
  const { slug } = useParams()
  const [n, setN] = useState(null)
  const [err, setErr] = useState('')
  useEffect(() => { api.get(`/api/public/tin-tuc/${slug}`).then((r) => setN(r.data)).catch((e) => setErr(errMsg(e))) }, [slug])
  if (err) return <div className="container section"><Alert>{err}</Alert></div>
  if (!n) return <div className="container section"><Loading /></div>
  return (
    <div className="container section prose">
      <p><Link to="/tin-tuc">← Tin tức</Link></p>
      <h1>{n.tieuDe}</h1>
      <p className="muted small">{fmtDate(n.ngayDang)} · {n.luotXem} lượt xem</p>
      {n.anhBia && <img src={imgUrl(n.anhBia)} alt="" />}
      {n.tomTat && <p><b>{n.tomTat}</b></p>}
      {/* Nội dung HTML do quản trị viên soạn */}
      <div dangerouslySetInnerHTML={{ __html: n.noiDung }} />
    </div>
  )
}
