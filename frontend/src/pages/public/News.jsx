import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { fmtDate, imgUrl } from '../../api'
import { Pagination, Loading } from '../../components/ui.jsx'

export default function News() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  useEffect(() => { api.get('/api/public/tin-tuc', { params: { page, size: 9 } }).then((r) => setData(r.data)).catch(() => setData({ content: [] })) }, [page])
  return (
    <>
      <div className="page-head"><div className="container"><h1>Tin tức</h1></div></div>
      <div className="container">
        {!data ? <Loading /> : data.content.length === 0 ? <p className="muted">Chưa có bài viết.</p> : (
          <div className="grid grid-3">
            {data.content.map((n) => (
              <Link key={n.tinTucId} to={`/tin-tuc/${n.slug}`} className="card flush" style={{ color: 'inherit' }}>
                <div style={{ aspectRatio: '16/9', background: `var(--green-soft) center/cover url(${imgUrl(n.anhBia) || ''})` }} />
                <div style={{ padding: 16 }}>
                  <div className="muted small">{fmtDate(n.ngayDang)} · {n.luotXem} lượt xem</div>
                  <h3 style={{ fontSize: '1.05rem' }}>{n.tieuDe}</h3>
                  <p className="muted small">{n.tomTat}</p>
                </div>
              </Link>
            ))}
          </div>
        )}
        {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
      </div>
    </>
  )
}
