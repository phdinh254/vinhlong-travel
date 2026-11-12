import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { money, fmtDateTime, TRANG_THAI_DON } from '../../api'
import { Badge, Loading } from '../../components/ui.jsx'

export default function Dashboard() {
  const [d, setD] = useState(null)
  useEffect(() => { api.get('/api/admin/dashboard').then((r) => setD(r.data)) }, [])
  if (!d) return <Loading />
  const stats = [
    ['Tour', d.tongTour, '/admin/tours'], ['Tổng đơn', d.tongDon, '/admin/don-dat'],
    ['Chờ thanh toán', d.donChoThanhToan, '/admin/don-dat?trangThai=CHO_THANH_TOAN'], ['Đã cọc', d.donDaCoc, '/admin/don-dat?trangThai=DA_COC'],
    ['Đã thanh toán', d.donDaThanhToan, '/admin/don-dat?trangThai=DA_THANH_TOAN'], ['Tổng tiền đã thu (ròng)', money(d.tongDaThu), null],
    ['Yêu cầu hủy chờ xử lý', d.yeuCauHuyCho, '/admin/yeu-cau-huy'], ['Liên hệ mới', d.lienHeMoi, '/admin/lien-he'],
  ]
  return (
    <>
      <div className="admin-head"><h1>Tổng quan</h1></div>
      <div className="grid grid-4">
        {stats.map(([l, v, to]) => (
          <div key={l} className="card stat">
            <div className="muted small">{l}</div>
            <div className="num">{to ? <Link to={to} style={{ color: 'inherit' }}>{v}</Link> : v}</div>
          </div>
        ))}
      </div>
      <h3 style={{ marginTop: 28 }}>Đơn mới nhất</h3>
      <div className="table-wrap"><table>
        <thead><tr><th>Mã đơn</th><th>Tour</th><th>Người đặt</th><th>Khởi hành</th><th className="right">Tổng</th><th>Trạng thái</th><th>Ngày đặt</th></tr></thead>
        <tbody>{d.donGanDay.map((x) => (
          <tr key={x.donDatTourId}><td><Link to={`/admin/don-dat/${x.donDatTourId}`}>{x.maDon}</Link></td><td>{x.tenTour}</td><td>{x.tenNguoiDat}<div className="muted small">{x.sdtNguoiDat}</div></td>
            <td>{fmtDateTime(x.ngayGioDi)}</td><td className="right">{money(x.tongTien)}</td><td><Badge map={TRANG_THAI_DON} value={x.trangThai} /></td><td>{fmtDateTime(x.ngayDat)}</td></tr>
        ))}</tbody>
      </table></div>
    </>
  )
}
