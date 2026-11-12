import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import api, { errMsg, money, fmtDateTime } from '../../api'
import { Alert, Loading, Modal, ImagePicker } from '../../components/ui.jsx'
import BookingInfo from '../../components/BookingInfo.jsx'

/** Admin xử lý đơn: xác nhận thanh toán thủ công, duyệt hủy, hoàn tiền, chuyển đợt, đổi trạng thái */
export default function BookingDetail() {
  const { id } = useParams()
  const [d, setD] = useState(null)
  const [err, setErr] = useState('')
  const [modal, setModal] = useState(null)

  const load = () => api.get(`/api/admin/bookings/${id}`).then((r) => setD(r.data)).catch((e) => setErr(errMsg(e)))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { load() }, [id])

  if (!d) return err ? <Alert>{err}</Alert> : <Loading />
  const active = ['CHO_THANH_TOAN', 'DA_COC', 'DA_THANH_TOAN'].includes(d.trangThai)
  const pending = d.yeuCauHuys.find((y) => y.ketQua === 'CHO_XU_LY')
  const done = (data) => { setD(data); setModal(null) }

  return (
    <>
      <div className="admin-head">
        <h1>Đơn {d.maDon}</h1>
        <div className="row">
          <Link className="btn secondary" to="/admin/don-dat">← Danh sách</Link>
          {(active || d.trangThai === 'HET_HAN') && Number(d.conLai) > 0 && <button className="btn" onClick={() => setModal('pay')}>Xác nhận thanh toán</button>}
          {Number(d.daThanhToan) > 0 && <button className="btn secondary" onClick={() => setModal('refund')}>Hoàn tiền</button>}
          {active && <button className="btn secondary" onClick={() => setModal('transfer')}>Chuyển đợt</button>}
          {d.trangThai === 'DA_THANH_TOAN' && <button className="btn secondary" onClick={() => setModal('done')}>Hoàn thành</button>}
          {active && <button className="btn danger" onClick={() => setModal('cancel')}>Hủy đơn</button>}
        </div>
      </div>
      <Alert>{err}</Alert>
      {pending && (
        <div className="alert" style={{ marginBottom: 14 }}>
          Có yêu cầu {pending.loaiHuy === 'TOAN_BO' ? 'hủy toàn bộ' : `hủy ${pending.hanhKhachs.length} hành khách`} chờ xử lý (phí phạt {money(pending.tongPhiPhat)}, dự kiến hoàn {money(pending.soTienDuKienHoan)}).{' '}
          <button className="btn sm" onClick={() => setModal({ type: 'process', y: pending })}>Xử lý</button>
        </div>
      )}
      <BookingInfo d={d} admin />

      {modal === 'pay' && <PayModal d={d} onClose={() => setModal(null)} onDone={done} />}
      {modal === 'refund' && <RefundModal d={d} onClose={() => setModal(null)} onDone={done} />}
      {modal === 'transfer' && <TransferModal d={d} onClose={() => setModal(null)} onDone={done} />}
      {(modal === 'done' || modal === 'cancel') && <StatusModal d={d} trangThai={modal === 'done' ? 'HOAN_THANH' : 'DA_HUY'} onClose={() => setModal(null)} onDone={done} />}
      {modal?.type === 'process' && <ProcessModal y={modal.y} onClose={() => setModal(null)} onDone={done} />}
    </>
  )
}

function useSubmit(onDone) {
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const run = async (fn) => { setBusy(true); setErr(''); try { onDone((await fn()).data) } catch (e) { setErr(errMsg(e)) } finally { setBusy(false) } }
  return { err, busy, run }
}

function PayModal({ d, onClose, onDone }) {
  const [f, setF] = useState({ soTien: d.conLai, loaiThanhToan: Number(d.daThanhToan) > 0 ? 'TAT_TOAN' : 'TOAN_BO', phuongThuc: 'CHUYEN_KHOAN', maGiaoDich: '', anhChungTu: '' })
  const s = useSubmit(onDone)
  return (
    <Modal title="Xác nhận thanh toán thủ công (Q33–Q34)" onClose={onClose}>
      <div className="grid grid-3">
        <div className="field"><label>Số tiền</label><input type="number" value={f.soTien} onChange={(e) => setF({ ...f, soTien: e.target.value })} /></div>
        <div className="field"><label>Loại</label><select value={f.loaiThanhToan} onChange={(e) => setF({ ...f, loaiThanhToan: e.target.value })}><option value="COC">Cọc</option><option value="TAT_TOAN">Tất toán</option><option value="TOAN_BO">Toàn bộ</option></select></div>
        <div className="field"><label>Phương thức</label><select value={f.phuongThuc} onChange={(e) => setF({ ...f, phuongThuc: e.target.value })}><option value="CHUYEN_KHOAN">Chuyển khoản</option><option value="TIEN_MAT">Tiền mặt</option></select></div>
      </div>
      <div className="field"><label>Mã giao dịch ngân hàng</label><input value={f.maGiaoDich} onChange={(e) => setF({ ...f, maGiaoDich: e.target.value })} /></div>
      <ImagePicker label="Ảnh chứng từ / biên lai" value={f.anhChungTu} onChange={(v) => setF({ ...f, anhChungTu: v })} />
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" disabled={s.busy} onClick={() => s.run(() => api.post(`/api/admin/bookings/${d.donDatTourId}/thanh-toan`, { ...f, soTien: Number(f.soTien) }))}>Xác nhận</button></div>
    </Modal>
  )
}

function RefundModal({ d, onClose, onDone }) {
  const accepted = d.yeuCauHuys.filter((y) => y.ketQua === 'CHAP_NHAN')
  const [f, setF] = useState({ yeuCauHuyId: accepted[0]?.yeuCauHuyId || '', soTienHoan: accepted[0]?.soTienDuKienHoan || '', phuongThuc: 'CHUYEN_KHOAN', maGiaoDichHoan: '', anhChungTu: '', ghiChu: '' })
  const s = useSubmit(onDone)
  return (
    <Modal title="Ghi nhận hoàn tiền (Q38)" onClose={onClose}>
      <p className="muted small">Đã thu ròng: {money(d.daThanhToan)}. Có thể hoàn nhiều lần.</p>
      <div className="grid grid-3">
        <div className="field"><label>Theo yêu cầu hủy</label><select value={f.yeuCauHuyId} onChange={(e) => setF({ ...f, yeuCauHuyId: e.target.value })}><option value="">— Không —</option>{accepted.map((y) => <option key={y.yeuCauHuyId} value={y.yeuCauHuyId}>#{y.yeuCauHuyId} – dự kiến {money(y.soTienDuKienHoan)}</option>)}</select></div>
        <div className="field"><label>Số tiền hoàn</label><input type="number" value={f.soTienHoan} onChange={(e) => setF({ ...f, soTienHoan: e.target.value })} /></div>
        <div className="field"><label>Phương thức</label><select value={f.phuongThuc} onChange={(e) => setF({ ...f, phuongThuc: e.target.value })}><option value="CHUYEN_KHOAN">Chuyển khoản</option><option value="TIEN_MAT">Tiền mặt</option><option value="VNPAY">VNPay</option></select></div>
      </div>
      <div className="field"><label>Mã giao dịch hoàn</label><input value={f.maGiaoDichHoan} onChange={(e) => setF({ ...f, maGiaoDichHoan: e.target.value })} /></div>
      <ImagePicker label="Ảnh chứng từ hoàn tiền" value={f.anhChungTu} onChange={(v) => setF({ ...f, anhChungTu: v })} />
      <div className="field"><label>Ghi chú</label><input value={f.ghiChu} onChange={(e) => setF({ ...f, ghiChu: e.target.value })} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" disabled={s.busy} onClick={() => s.run(() => api.post(`/api/admin/bookings/${d.donDatTourId}/hoan-tien`, { ...f, yeuCauHuyId: f.yeuCauHuyId ? Number(f.yeuCauHuyId) : null, soTienHoan: Number(f.soTienHoan) }))}>Ghi nhận</button></div>
    </Modal>
  )
}

function TransferModal({ d, onClose, onDone }) {
  const [dots, setDots] = useState([])
  const [f, setF] = useState({ dotMoiId: '', lyDo: '' })
  const s = useSubmit(onDone)
  useEffect(() => { api.get(`/api/admin/tours/${d.tourId}`).then((r) => setDots(r.data.dots.filter((x) => x.dotKhoiHanhId !== d.dotKhoiHanhId && x.trangThai === 'MO_BAN'))) }, [d])
  return (
    <Modal title="Chuyển đợt khởi hành (Q39)" onClose={onClose}>
      <p className="muted small">Giá vé của hành khách được tính lại theo bảng giá đợt mới; chênh lệch được ghi vào lịch sử chuyển đợt.</p>
      <div className="field"><label>Đợt mới</label><select value={f.dotMoiId} onChange={(e) => setF({ ...f, dotMoiId: e.target.value })}><option value="">— Chọn —</option>{dots.map((x) => <option key={x.dotKhoiHanhId} value={x.dotKhoiHanhId}>{x.maDot} · {fmtDateTime(x.ngayGioDi)} · còn {x.soChoConLai} chỗ</option>)}</select></div>
      <div className="field"><label>Lý do</label><input value={f.lyDo} onChange={(e) => setF({ ...f, lyDo: e.target.value })} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn" disabled={s.busy || !f.dotMoiId} onClick={() => s.run(() => api.post(`/api/admin/bookings/${d.donDatTourId}/chuyen-dot`, { dotMoiId: Number(f.dotMoiId), lyDo: f.lyDo }))}>Chuyển đợt</button></div>
    </Modal>
  )
}

function StatusModal({ d, trangThai, onClose, onDone }) {
  const [lyDo, setLyDo] = useState('')
  const s = useSubmit(onDone)
  return (
    <Modal title={trangThai === 'DA_HUY' ? 'Hủy đơn (trả chỗ)' : 'Đánh dấu hoàn thành'} onClose={onClose}>
      {trangThai === 'DA_HUY' && <p className="alert small">Hủy trực tiếp sẽ trả toàn bộ chỗ về đợt. Nếu khách đã thanh toán, hãy ghi nhận hoàn tiền sau khi hủy.</p>}
      <div className="field"><label>Lý do</label><input value={lyDo} onChange={(e) => setLyDo(e.target.value)} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}><button className={`btn ${trangThai === 'DA_HUY' ? 'danger' : ''}`} disabled={s.busy} onClick={() => s.run(() => api.put(`/api/admin/bookings/${d.donDatTourId}/trang-thai`, { trangThai, lyDo }))}>Xác nhận</button></div>
    </Modal>
  )
}

export function ProcessModal({ y, onClose, onDone }) {
  const [ghiChu, setGhiChu] = useState('')
  const s = useSubmit(onDone)
  return (
    <Modal title={`Xử lý yêu cầu hủy – đơn ${y.maDon}`} onClose={onClose}>
      <p><b>{y.loaiHuy === 'TOAN_BO' ? 'Hủy toàn bộ' : 'Hủy từng phần'}</b> · {y.hanhKhachs.join(', ')}</p>
      <p className="small">Lý do: {y.lyDo || '—'}</p>
      <p className="small">Phí phạt theo chính sách: <b>{money(y.tongPhiPhat)}</b> · Dự kiến hoàn: <b>{money(y.soTienDuKienHoan)}</b></p>
      <div className="field"><label>Phản hồi cho khách</label><textarea value={ghiChu} onChange={(e) => setGhiChu(e.target.value)} /></div>
      <Alert>{s.err}</Alert>
      <div className="row" style={{ justifyContent: 'flex-end' }}>
        <button className="btn secondary" disabled={s.busy} onClick={() => s.run(() => api.put(`/api/admin/yeu-cau-huy/${y.yeuCauHuyId}`, { chapNhan: false, ghiChu }))}>Từ chối</button>
        <button className="btn danger" disabled={s.busy} onClick={() => s.run(() => api.put(`/api/admin/yeu-cau-huy/${y.yeuCauHuyId}`, { chapNhan: true, ghiChu }))}>Chấp nhận hủy</button>
      </div>
    </Modal>
  )
}
