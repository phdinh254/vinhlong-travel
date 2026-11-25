import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import api, { errMsg, fmtDate, imgUrl } from '../../api'
import { Alert, Loading } from '../../components/ui.jsx'
import { tenChuyenMuc, anhMacDinh, phutDoc } from '../../content/chuyenMuc.js'
import './news.css'

export default function NewsDetail() {
  const { slug } = useParams()
  const [n, setN] = useState(null)
  const [err, setErr] = useState('')
  useEffect(() => { api.get(`/api/v1/articles/${slug}`).then((r) => setN(r.data)).catch((e) => setErr(errMsg(e))) }, [slug])
  if (err) return <div className="container section"><Alert>{err}</Alert></div>
  if (!n) return <div className="container section"><Loading /></div>
  return (
    <article className="nw nw-article">
      <header className="container nw-article-head">
        <Link to={`/tin-tuc?chuyenMuc=${n.category || 'NEWS'}`} className="nw-back">← {tenChuyenMuc(n.category)}</Link>
        <h1>{n.title}</h1>
        {n.summary && <p className="nw-lead">{n.summary}</p>}
        <div className="nw-meta"><span>{fmtDate(n.publishedAt)}</span><span>{phutDoc(n.content)} phút đọc</span><span>{n.viewCount} lượt xem</span></div>
      </header>
      <div className="container">
        <img className="nw-article-cover" src={imgUrl(n.coverImageUrl) || anhMacDinh(n)} alt="" />
        {/* Nội dung HTML do quản trị viên soạn */}
        <div className="prose nw-article-body" dangerouslySetInnerHTML={{ __html: n.content }} />
        <p className="nw-article-foot"><Link to="/tin-tuc">← Về trang Tin tức</Link></p>
      </div>
    </article>
  )
}
