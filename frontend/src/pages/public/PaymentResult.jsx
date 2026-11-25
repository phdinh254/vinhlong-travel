import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import api, { errMsg, money } from '../../api'
import { Loading } from '../../components/ui.jsx'

/** Trang payOS chuyển về (returnUrl/cancelUrl, có ?orderCode=...): backend hỏi lại payOS trạng thái thật rồi ghi nhận */
export default function PaymentResult() {
  const [sp] = useSearchParams()
  const [r, setR] = useState(null)
  const [err, setErr] = useState('')

  useEffect(() => {
    api.get('/api/v1/payments/payos/return', { params: { orderCode: sp.get('orderCode') } })
      .then((x) => setR(x.data)).catch((e) => setErr(errMsg(e)))
  }, [sp])

  const ok = r?.paid
  return (
    <div className="container section" style={{ maxWidth: 560 }}>
      <div className="card" style={{ textAlign: 'center' }}>
        {err ? <p className="alert error">{err}</p> : !r ? <Loading /> : (
          <>
            <div style={{ fontSize: 48 }}>{ok ? '✅' : '⚠️'}</div>
            <h2>{r.message || (ok ? 'Thanh toán thành công' : 'Thanh toán chưa thành công')}</h2>
            {r.amount != null && <p>Số tiền: <b>{money(r.amount)}</b></p>}
            {r.transactionNo && <p className="muted small">Mã giao dịch: {r.transactionNo}</p>}
            {r.bookingCode && <p><Link className="btn" to={`/don/${r.bookingCode}`}>Xem đơn {r.bookingCode}</Link></p>}
            <p className="muted small">Nếu đã trừ tiền nhưng đơn chưa cập nhật, vui lòng chờ vài phút hoặc liên hệ hotline.</p>
          </>
        )}
      </div>
    </div>
  )
}
