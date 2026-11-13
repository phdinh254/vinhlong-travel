import { useCallback, useEffect, useState } from 'react'
import { useParams, useSearchParams, useLocation } from 'react-router-dom'
import api, { errMsg, money } from '../../api'
import { Alert, Loading, Modal } from '../../components/ui.jsx'
import BookingInfo from '../../components/BookingInfo.jsx'

/** Xem đơn + thanh toán VNPay + gửi yêu cầu hủy (khách đăng nhập hoặc vãng lai qua SĐT) */
export default function BookingView() {
  const { maDon } = useParams()
  const [sp] = useSearchParams()
  // Khách vãng lai: nhớ SĐT trong phiên để quay lại từ trang kết quả VNPay vẫn xem được đơn
  const sdt = sp.get('sdt') || sessionStorage.getItem(`sdt_${maDon}`) || ''
  useEffect(() => { if (sdt) sessionStorage.setItem(`sdt_${maDon}`, sdt) }, [maDon, sdt])
  const loc = useLocation()
  const [d, setD] = useState(null)
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const [showHuy, setShowHuy] = useState(false)
  const [huy, setHuy] = useState({ loaiHuy: 'TOAN_BO', lyDo: '', hanhKhachIds: [] })
  const [huyErr, setHuyErr] = useState('')

  const load = useCallback(() => {
    api.get(`/api/bookings/${maDon}`, { params: { sdt } }).then((r) => setD(r.data)).catch((e) => setErr(errMsg(e)))
  }, [maDon, sdt])
  useEffect(load, [load])

  const pay = async (loai) => {
    setBusy(true); setErr('')
    try {
      const { data } = await api.post('/api/payments/vnpay/create', { maDon, sdt, loai })
      window.location.href = data.paymentUrl
    } catch (e) { setErr(errMsg(e)); setBusy(false) }
  }

  const guiHuy = async () => {
    setHuyErr('')
    try {
      const { data } = await api.post(`/api/bookings/${maDon}/yeu-cau-huy`, { ...huy, sdt })
      setD(data); setShowHuy(false)
    } catch (e) { setHuyErr(errMsg(e)) }
  }

  if (err && !d) return <div className="container section"><Alert>{err}</Alert></div>
  if (!d) return <div className="container section"><Loading /></div>

  const coTheTT = ['CHO_THANH_TOAN', 'DA_COC'].includes(d.trangThai) && Number(d.conLai) > 0
  const coTheHuy = ['CHO_THANH_TOAN', 'DA_COC', 'DA_THANH_TOAN'].includes(d.trangThai)
    && !d.yeuCauHuys.some((y) => y.ketQua === 'CHO_XU_LY') && new Date(d.ngayGioDi) > new Date()
  const hkHieuLuc = d.hanhKhachs.filter((h) => h.trangThai === 'HIEU_LUC')

  return (
    <div className="container section grid grid-main">
      <div>
        {loc.state?.moi && <div className="alert ok" style={{ marginBottom: 12 }}>Đặt tour thành công! Hãy lưu lại mã đơn <b>{d.maDon}</b> và số điện thoại để tra cứu.</div>}
        <BookingInfo d={d} />
      </div>
      <aside className="stack">
        <div className="card" style={{ position: 'sticky', top: 80 }}>
          <h3>Thanh toán</h3>
          <Alert>{err}</Alert>
          {coTheTT ? (
            <>
              <p className="small">Còn phải thanh toán: <b className="price">{money(d.conLai)}</b></p>
              {Number(d.daThanhToan) === 0 && Number(d.soTienCocToiThieu) < Number(d.tongTien) && (
                <button className="btn secondary" style={{ width: '100%', marginBottom: 8 }} disabled={busy} onClick={() => pay('COC')}>
                  Đặt cọc {money(d.soTienCocToiThieu)} qua VNPay
                </button>
              )}
              <button className="btn" style={{ width: '100%' }} disabled={busy} onClick={() => pay('CON_LAI')}>
                Thanh toán {money(d.conLai)} qua VNPay
              </button>
              <p className="muted small">Bạn sẽ được chuyển sang cổng VNPay để thanh toán.</p>
            </>
          ) : <p className="muted small">Đơn không cần thanh toán thêm.</p>}
          {coTheHuy && (
            <>
              <hr style={{ border: 0, borderTop: '1px solid var(--line)', margin: '16px 0' }} />
              <button className="btn danger sm" onClick={() => setShowHuy(true)}>Gửi yêu cầu hủy</button>
            </>
          )}
        </div>
      </aside>

      {showHuy && (
        <Modal title="Yêu cầu hủy tour" onClose={() => setShowHuy(false)}>
          <div className="field">
            <label className="check"><input type="radio" checked={huy.loaiHuy === 'TOAN_BO'} onChange={() => setHuy({ ...huy, loaiHuy: 'TOAN_BO' })} /> Hủy toàn bộ đơn</label>
            {hkHieuLuc.length > 1 && <label className="check"><input type="radio" checked={huy.loaiHuy === 'TUNG_PHAN'} onChange={() => setHuy({ ...huy, loaiHuy: 'TUNG_PHAN' })} /> Hủy một số hành khách</label>}
          </div>
          {huy.loaiHuy === 'TUNG_PHAN' && (
            <div className="box field">
              {hkHieuLuc.map((h) => (
                <label key={h.hanhKhachId} className="check">
                  <input type="checkbox" checked={huy.hanhKhachIds.includes(h.hanhKhachId)}
                    onChange={(e) => setHuy({ ...huy, hanhKhachIds: e.target.checked ? [...huy.hanhKhachIds, h.hanhKhachId] : huy.hanhKhachIds.filter((x) => x !== h.hanhKhachId) })} />
                  {h.hoTen} ({h.tenLoaiKhach})
                </label>
              ))}
            </div>
          )}
          <div className="field"><label>Lý do</label><textarea value={huy.lyDo} onChange={(e) => setHuy({ ...huy, lyDo: e.target.value })} /></div>
          <p className="muted small">Phí phạt được tính theo chính sách hủy đã áp dụng khi bạn đặt tour. Đơn chưa thanh toán sẽ được hủy ngay.</p>
          <Alert>{huyErr}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button className="btn secondary" onClick={() => setShowHuy(false)}>Đóng</button>
            <button className="btn danger" onClick={guiHuy}>Gửi yêu cầu</button>
          </div>
        </Modal>
      )}
    </div>
  )
}
