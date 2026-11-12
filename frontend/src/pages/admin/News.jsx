import { useEffect, useState } from 'react'
import api, { errMsg, fmtDateTime } from '../../api'
import { Alert, Loading, Modal, ImagePicker } from '../../components/ui.jsx'

const empty = { tieuDe: '', slug: '', tomTat: '', noiDung: '', anhBia: '', trangThai: 'NHAP' }

export default function News() {
  const [list, setList] = useState(null)
  const [edit, setEdit] = useState(null)
  const [err, setErr] = useState('')
  const load = () => api.get('/api/admin/tin-tuc').then((r) => setList(r.data))
  useEffect(() => { load() }, [])

  const save = async () => {
    setErr('')
    try {
      const { tinTucId, ...body } = edit
      if (tinTucId) await api.put(`/api/admin/tin-tuc/${tinTucId}`, body); else await api.post('/api/admin/tin-tuc', body)
      setEdit(null); load()
    } catch (e) { setErr(errMsg(e)) }
  }
  const del = async (n) => { if (confirm(`Xóa bài "${n.tieuDe}"?`)) { await api.delete(`/api/admin/tin-tuc/${n.tinTucId}`); load() } }

  return (
    <>
      <div className="admin-head"><h1>Tin tức</h1><button className="btn" onClick={() => setEdit({ ...empty })}>+ Viết bài</button></div>
      {!list ? <Loading /> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Tiêu đề</th><th>Trạng thái</th><th>Lượt xem</th><th>Ngày đăng</th><th></th></tr></thead>
          <tbody>{list.map((n) => (
            <tr key={n.tinTucId}><td>{n.tieuDe}<div className="muted small">/{n.slug}</div></td>
              <td><span className={`badge ${n.trangThai === 'XUAT_BAN' ? 'ok' : 'muted'}`}>{n.trangThai === 'XUAT_BAN' ? 'Xuất bản' : 'Nháp'}</span></td>
              <td>{n.luotXem}</td><td className="small">{fmtDateTime(n.ngayDang)}</td>
              <td className="row"><button className="btn secondary sm" onClick={() => setEdit({ ...n, slug: n.slug, tomTat: n.tomTat || '', anhBia: n.anhBia || '' })}>Sửa</button><button className="btn link small" onClick={() => del(n)}>Xóa</button></td></tr>
          ))}</tbody>
        </table></div>
      )}
      {edit && (
        <Modal title={edit.tinTucId ? 'Sửa bài viết' : 'Viết bài mới'} onClose={() => setEdit(null)}>
          <div className="field"><label>Tiêu đề *</label><input value={edit.tieuDe} onChange={(e) => setEdit({ ...edit, tieuDe: e.target.value })} /></div>
          <div className="grid grid-2">
            <div className="field"><label>Slug (để trống = tự tạo)</label><input value={edit.slug} onChange={(e) => setEdit({ ...edit, slug: e.target.value })} /></div>
            <div className="field"><label>Trạng thái</label><select value={edit.trangThai} onChange={(e) => setEdit({ ...edit, trangThai: e.target.value })}><option value="NHAP">Nháp</option><option value="XUAT_BAN">Xuất bản</option></select></div>
          </div>
          <ImagePicker label="Ảnh bìa" value={edit.anhBia} onChange={(v) => setEdit({ ...edit, anhBia: v })} />
          <div className="field"><label>Tóm tắt</label><textarea value={edit.tomTat} onChange={(e) => setEdit({ ...edit, tomTat: e.target.value })} /></div>
          <div className="field"><label>Nội dung (HTML) *</label><textarea style={{ minHeight: 220 }} value={edit.noiDung} onChange={(e) => setEdit({ ...edit, noiDung: e.target.value })} /></div>
          <Alert>{err}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" onClick={save}>Lưu</button></div>
        </Modal>
      )}
    </>
  )
}
