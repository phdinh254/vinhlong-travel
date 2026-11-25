import { useEffect, useState } from 'react'
import api, { errMsg, fmtDateTime, pageCount } from '../../api'
import { Alert, Loading, Modal, ImagePicker, Pagination } from '../../components/ui.jsx'
import { CHUYEN_MUC, tenChuyenMuc } from '../../content/chuyenMuc.js'

const empty = { title: '', slug: '', summary: '', content: '', coverImageUrl: '', status: 'DRAFT', category: 'NEWS' }

export default function News() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [edit, setEdit] = useState(null)
  const [err, setErr] = useState('')
  const load = () => api.get('/api/v1/admin/articles', { params: { page, size: 20 } }).then((r) => setData(r.data)).catch((e) => setErr(errMsg(e)))
  useEffect(() => { load() }, [page]) // eslint-disable-line react-hooks/exhaustive-deps

  const open = async (n) => {
    setErr('')
    try {
      const { data: a } = await api.get(`/api/v1/admin/articles/${n.id}`)
      setEdit({ id: a.id, title: a.title, slug: a.slug, summary: a.summary || '', content: a.content || '', coverImageUrl: a.coverImageUrl || '', status: a.status, category: a.category })
    } catch (e) { setErr(errMsg(e)) }
  }
  const save = async () => {
    setErr('')
    try {
      const { id, ...b } = edit
      const body = { ...b, slug: b.slug || null, summary: b.summary || null, coverImageUrl: b.coverImageUrl || null }
      if (id) await api.put(`/api/v1/admin/articles/${id}`, body); else await api.post('/api/v1/admin/articles', body)
      setEdit(null); load()
    } catch (e) { setErr(errMsg(e)) }
  }
  const del = async (n) => {
    if (!confirm(`Xóa bài "${n.title}"?`)) return
    try { await api.delete(`/api/v1/admin/articles/${n.id}`); load() } catch (e) { setErr(errMsg(e)) }
  }

  return (
    <>
      <div className="admin-head"><h1>Tin tức</h1><button className="btn" onClick={() => setEdit({ ...empty })}>+ Viết bài</button></div>
      {!edit && <Alert>{err}</Alert>}
      {!data ? <Loading /> : (
        <>
          <div className="table-wrap"><table>
            <thead><tr><th>Tiêu đề</th><th>Chuyên mục</th><th>Trạng thái</th><th>Lượt xem</th><th>Ngày đăng</th><th></th></tr></thead>
            <tbody>{data.items.map((n) => (
              <tr key={n.id}><td>{n.title}<div className="muted small">/{n.slug}</div></td>
                <td className="small">{tenChuyenMuc(n.category)}</td>
                <td><span className={`badge ${n.status === 'PUBLISHED' ? 'ok' : 'muted'}`}>{n.status === 'PUBLISHED' ? 'Xuất bản' : 'Nháp'}</span></td>
                <td>{n.viewCount}</td><td className="small">{fmtDateTime(n.publishedAt)}</td>
                <td className="row"><button className="btn secondary sm" onClick={() => open(n)}>Sửa</button><button className="btn link small" onClick={() => del(n)}>Xóa</button></td></tr>
            ))}</tbody>
          </table></div>
          <Pagination page={page} totalPages={pageCount(data)} onChange={setPage} />
        </>
      )}
      {edit && (
        <Modal title={edit.id ? 'Sửa bài viết' : 'Viết bài mới'} onClose={() => setEdit(null)}>
          <div className="field"><label>Tiêu đề *</label><input value={edit.title} onChange={(e) => setEdit({ ...edit, title: e.target.value })} /></div>
          <div className="grid grid-2">
            <div className="field"><label>Slug (để trống = tự tạo)</label><input value={edit.slug} disabled={Boolean(edit.id)} onChange={(e) => setEdit({ ...edit, slug: e.target.value })} /></div>
            <div className="field"><label>Trạng thái</label><select value={edit.status} onChange={(e) => setEdit({ ...edit, status: e.target.value })}><option value="DRAFT">Nháp</option><option value="PUBLISHED">Xuất bản</option></select></div>
          </div>
          <div className="field"><label>Chuyên mục</label><select value={edit.category} onChange={(e) => setEdit({ ...edit, category: e.target.value })}>{CHUYEN_MUC.map((c) => <option key={c.key} value={c.key}>{c.label}</option>)}</select></div>
          <ImagePicker label="Ảnh bìa" value={edit.coverImageUrl} onChange={(v) => setEdit({ ...edit, coverImageUrl: v })} />
          <div className="field"><label>Tóm tắt</label><textarea value={edit.summary} onChange={(e) => setEdit({ ...edit, summary: e.target.value })} /></div>
          <div className="field"><label>Nội dung (HTML) *</label><textarea style={{ minHeight: 220 }} value={edit.content} onChange={(e) => setEdit({ ...edit, content: e.target.value })} /></div>
          <Alert>{err}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" onClick={save}>Lưu</button></div>
        </Modal>
      )}
    </>
  )
}
