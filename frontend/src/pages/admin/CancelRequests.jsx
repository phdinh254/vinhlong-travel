import { useEffect, useState } from 'react'
import api, { money, fmtDateTime } from '../../api'
import { Badge, Loading } from '../../components/ui.jsx'
import { ProcessModal } from './BookingDetail.jsx'

const KQ = { CHO_XU_LY: ['Chờ xử lý', 'warn'], CHAP_NHAN: ['Chấp nhận', 'ok'], TU_CHOI: ['Từ chối', 'bad'] }

export default function CancelRequests() {
  const [list, setList] = useState(null)
  const [sel, setSel] = useState(null)
  const [chiCho, setChiCho] = useState(true)
  const load = () => api.get('/api/admin/yeu-cau-huy').then((r) => setList(r.data))
  useEffect(() => { load() }, [])
  const rows = (list || []).filter((y) => !chiCho || y.ketQua === 'CHO_XU_LY')
  return (
    <>
      <div className="admin-head"><h1>Yêu cầu hủy (Q35–Q37)</h1>
        <label className="check"><input type="checkbox" checked={chiCho} onChange={(e) => setChiCho(e.target.checked)} /> Chỉ hiện yêu cầu chờ xử lý</label></div>
      {!list ? <Loading /> : rows.length === 0 ? <p className="muted">Không có yêu cầu.</p> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Đơn</th><th>Loại</th><th>Hành khách</th><th>Lý do</th><th className="right">Phí phạt</th><th className="right">Dự kiến hoàn</th><th>Thời gian</th><th>Kết quả</th><th></th></tr></thead>
          <tbody>{rows.map((y) => (
            <tr key={y.yeuCauHuyId}><td>{y.maDon}</td><td>{y.loaiHuy === 'TOAN_BO' ? 'Toàn bộ' : 'Từng phần'}</td><td className="small">{y.hanhKhachs.join(', ')}</td>
              <td className="small">{y.lyDo}</td><td className="right">{money(y.tongPhiPhat)}</td><td className="right">{money(y.soTienDuKienHoan)}</td>
              <td className="small">{fmtDateTime(y.thoiGianYeuCau)}</td><td><Badge map={KQ} value={y.ketQua} /></td>
              <td>{y.ketQua === 'CHO_XU_LY' && <button className="btn sm" onClick={() => setSel(y)}>Xử lý</button>}</td></tr>
          ))}</tbody>
        </table></div>
      )}
      {sel && <ProcessModal y={sel} onClose={() => setSel(null)} onDone={() => { setSel(null); load() }} />}
    </>
  )
}
