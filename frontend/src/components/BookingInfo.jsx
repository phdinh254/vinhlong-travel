import { money, fmtDate, fmtDateTime, TRANG_THAI_DON } from '../api'
import { Badge } from './ui.jsx'

const TT_THANH_TOAN = { CHO_THANH_TOAN: ['Chờ', 'warn'], THANH_CONG: ['Thành công', 'ok'], THAT_BAI: ['Thất bại', 'bad'] }
const KQ_HUY = { CHO_XU_LY: ['Chờ xử lý', 'warn'], CHAP_NHAN: ['Chấp nhận', 'ok'], TU_CHOI: ['Từ chối', 'bad'] }

/** Hiển thị chi tiết đơn – dùng chung cho khách và admin */
export default function BookingInfo({ d, admin = false }) {
  return (
    <div className="stack">
      <div className="card">
        <div className="row between">
          <h2 style={{ margin: 0 }}>Đơn {d.maDon}</h2>
          <Badge map={TRANG_THAI_DON} value={d.trangThai} />
        </div>
        <p className="muted">{d.tenTour} · Đợt {d.maDot} · Khởi hành {fmtDateTime(d.ngayGioDi)}</p>
        <div className="grid grid-3 small">
          <div><b>Người đặt:</b> {d.tenNguoiDat}<br /><b>SĐT:</b> {d.sdtNguoiDat}<br /><b>Email:</b> {d.emailNguoiDat || '—'}</div>
          <div><b>Ngày đặt:</b> {fmtDateTime(d.ngayDat)}<br />{d.trangThai === 'CHO_THANH_TOAN' && <><b>Giữ chỗ đến:</b> {fmtDateTime(d.hanGiuCho)}<br /></>}<b>Số chỗ:</b> {d.soChoChiem}</div>
          <div>{admin && <><b>Loại khách:</b> {d.laKhachVangLai ? 'Vãng lai' : 'Có tài khoản'}<br /></>}<b>Ghi chú:</b> {d.ghiChu || '—'}</div>
        </div>
      </div>

      <div className="card">
        <h3>Hành khách</h3>
        <div className="table-wrap"><table>
          <thead><tr><th>Họ tên</th><th>Ngày sinh</th><th>Loại khách</th><th>Điểm đón</th><th className="right">Giá vé</th><th className="right">Phí đón</th><th>Trạng thái</th></tr></thead>
          <tbody>{d.hanhKhachs.map((h) => (
            <tr key={h.hanhKhachId} style={h.trangThai === 'DA_HUY' ? { opacity: .5, textDecoration: 'line-through' } : undefined}>
              <td>{h.hoTen}{admin && h.cccdPassport && <div className="muted small">{h.cccdPassport}</div>}</td>
              <td>{fmtDate(h.ngaySinh)}</td><td>{h.tenLoaiKhach}</td><td>{h.tenDiemDon || '—'}</td>
              <td className="right">{money(h.giaApDung)}</td><td className="right">{money(h.phiDon)}</td>
              <td>{h.trangThai === 'DA_HUY' ? 'Đã hủy' : 'Hiệu lực'}</td>
            </tr>
          ))}</tbody>
        </table></div>
        {d.phuThus.length > 0 && (
          <div style={{ marginTop: 10 }}>{d.phuThus.map((p) => <div key={p.phuThuId} className="summary-line small"><span>{p.tenPhuThu} × {p.soLuong}</span><span>{money(p.thanhTien)}</span></div>)}</div>
        )}
        <div style={{ maxWidth: 360, marginLeft: 'auto', marginTop: 10 }}>
          <div className="summary-line"><span>Tiền vé</span><span>{money(d.tongTienVe)}</span></div>
          <div className="summary-line"><span>Phụ thu</span><span>{money(d.tongPhuThu)}</span></div>
          <div className="summary-line total"><span>Tổng cộng</span><span>{money(d.tongTien)}</span></div>
          <div className="summary-line"><span>Đã thanh toán</span><span style={{ color: 'var(--green)' }}>{money(d.daThanhToan)}</span></div>
          <div className="summary-line"><b>Còn lại</b><b className="price">{money(d.conLai)}</b></div>
        </div>
      </div>

      {d.thanhToans.length > 0 && (
        <div className="card">
          <h3>Lịch sử thanh toán</h3>
          <div className="table-wrap"><table>
            <thead><tr><th>Thời gian</th><th>Loại</th><th>Phương thức</th><th>Mã GD</th><th className="right">Số tiền</th><th>Trạng thái</th>{admin && <th>Xác nhận</th>}</tr></thead>
            <tbody>{d.thanhToans.map((t) => (
              <tr key={t.thanhToanId}><td>{fmtDateTime(t.thoiGianTT || t.ngayTao)}</td><td>{t.loaiThanhToan}</td><td>{t.phuongThuc}</td>
                <td>{t.maGiaoDich || '—'}{t.anhChungTu && <> · <a href={t.anhChungTu.startsWith('http') ? t.anhChungTu : (import.meta.env.VITE_API_URL || 'http://localhost:8081') + t.anhChungTu} target="_blank" rel="noreferrer">chứng từ</a></>}</td>
                <td className="right">{money(t.soTien)}</td><td><Badge map={TT_THANH_TOAN} value={t.trangThai} /></td>{admin && <td>{t.nguoiXacNhan || '—'}</td>}</tr>
            ))}</tbody>
          </table></div>
        </div>
      )}

      {d.yeuCauHuys.length > 0 && (
        <div className="card">
          <h3>Yêu cầu hủy</h3>
          {d.yeuCauHuys.map((y) => (
            <div key={y.yeuCauHuyId} className="box" style={{ marginBottom: 8 }}>
              <div className="row between"><b>{y.loaiHuy === 'TOAN_BO' ? 'Hủy toàn bộ' : 'Hủy từng phần'} · {fmtDateTime(y.thoiGianYeuCau)}</b><Badge map={KQ_HUY} value={y.ketQua} /></div>
              <div className="small">Hành khách: {y.hanhKhachs.join(', ')}</div>
              <div className="small">Lý do: {y.lyDo || '—'}</div>
              <div className="small">Phí phạt: <b>{money(y.tongPhiPhat)}</b> · Dự kiến hoàn: <b>{money(y.soTienDuKienHoan)}</b></div>
              {y.ghiChuXuLy && <div className="small muted">Phản hồi: {y.ghiChuXuLy}</div>}
            </div>
          ))}
        </div>
      )}

      {d.hoanTiens.length > 0 && (
        <div className="card">
          <h3>Hoàn tiền</h3>
          {d.hoanTiens.map((h) => <div key={h.hoanTienId} className="summary-line small"><span>{fmtDateTime(h.ngayHoan)} · {h.phuongThuc} {h.maGiaoDichHoan && `· ${h.maGiaoDichHoan}`}</span><b>{money(h.soTienHoan)}</b></div>)}
        </div>
      )}

      {d.chuyenDots.length > 0 && (
        <div className="card">
          <h3>Lịch sử chuyển đợt</h3>
          {d.chuyenDots.map((c, i) => <div key={i} className="small">{fmtDateTime(c.thoiGian)}: {c.dotCu} → {c.dotMoi} (chênh lệch {money(c.chenhLechTien)}) {c.lyDo && `– ${c.lyDo}`}</div>)}
        </div>
      )}

      <div className="card">
        <h3>Lịch sử trạng thái</h3>
        {d.lichSu.map((l, i) => (
          <div key={i} className="small" style={{ padding: '4px 0', borderBottom: '1px dashed var(--line)' }}>
            {fmtDateTime(l.thoiGian)} · {l.trangThaiCu ? `${TRANG_THAI_DON[l.trangThaiCu]?.[0] || l.trangThaiCu} → ` : ''}<b>{TRANG_THAI_DON[l.trangThaiMoi]?.[0] || l.trangThaiMoi}</b> · {l.nguoiThayDoi}{l.lyDo && <span className="muted"> – {l.lyDo}</span>}
          </div>
        ))}
      </div>
    </div>
  )
}
