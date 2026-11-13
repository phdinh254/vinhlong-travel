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
  useEffect(() => { api.get(`/api/public/tin-tuc/${slug}`).then((r) => setN(r.data)).catch((e) => setErr(errMsg(e))) }, [slug])
  if (err) return <div className="container section"><Alert>{err}</Alert></div>
  if (!n) return <div className="container section"><Loading /></div>
  return (
    <article className="nw nw-article">
      <header className="container nw-article-head">
        <Link to={`/tin-tuc?chuyenMuc=${n.chuyenMuc || 'TIN_TUC'}`} className="nw-back">← {tenChuyenMuc(n.chuyenMuc)}</Link>
        <h1>{n.tieuDe}</h1>
        {n.tomTat && <p className="nw-lead">{n.tomTat}</p>}
        <div className="nw-meta"><span>{fmtDate(n.ngayDang)}</span><span>{phutDoc(n.noiDung)} phút đọc</span><span>{n.luotXem} lượt xem</span></div>
      </header>
      <div className="container">
        <img className="nw-article-cover" src={imgUrl(n.anhBia) || anhMacDinh(n)} alt="" />
        {/* Nội dung HTML do quản trị viên soạn */}
        <div className="prose nw-article-body" dangerouslySetInnerHTML={{ __html: n.noiDung }} />
        <p className="nw-article-foot"><Link to="/tin-tuc">← Về trang Tin tức</Link></p>
      </div>
    </article>
  )
}
