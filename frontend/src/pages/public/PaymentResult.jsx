import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import api, { errMsg, money } from '../../api'
import { Loading } from '../../components/ui.jsx'

/** Trang VNPay chuyển về (vnp_ReturnUrl): gửi nguyên query string cho backend kiểm tra chữ ký */
export default function PaymentResult() {
  const [sp] = useSearchParams()
  const [r, setR] = useState(null)
  const [err, setErr] = useState('')

  useEffect(() => {
    api.get(`/api/payments/vnpay/return?${sp.toString()}`).then((x) => setR(x.data)).catch((e) => setErr(errMsg(e)))
  }, [sp])

  return (
    <div className="container section" style={{ maxWidth: 560 }}>
      <div className="card" style={{ textAlign: 'center' }}>
        {err ? <p className="alert error">{err}</p> : !r ? <Loading /> : (
          <>
            <div style={{ fontSize: 48 }}>{r.hopLe && r.thanhCong ? '✅' : '⚠️'}</div>
            <h2>{r.thongBao}</h2>
            {r.soTien && <p>Số tiền: <b>{money(r.soTien)}</b></p>}
            {r.maGiaoDich && <p className="muted small">Mã giao dịch VNPay: {r.maGiaoDich}</p>}
            {r.maDon && <p><Link className="btn" to={`/don/${r.maDon}`}>Xem đơn {r.maDon}</Link></p>}
            <p className="muted small">Nếu đã trừ tiền nhưng đơn chưa cập nhật, vui lòng chờ vài phút hoặc liên hệ hotline.</p>
          </>
        )}
      </div>
    </div>
  )
}
