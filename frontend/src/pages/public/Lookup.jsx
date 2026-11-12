import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

/** Khách vãng lai tra cứu đơn bằng mã đơn + số điện thoại người đặt */
export default function Lookup() {
  const [maDon, setMaDon] = useState('')
  const [sdt, setSdt] = useState('')
  const nav = useNavigate()
  return (
    <>
      <div className="page-head"><div className="container"><h1>Tra cứu đơn đặt tour</h1></div></div>
      <div className="container">
        <form className="card" style={{ maxWidth: 480 }} onSubmit={(e) => { e.preventDefault(); nav(`/don/${maDon.trim().toUpperCase()}?sdt=${encodeURIComponent(sdt.trim())}`) }}>
          <div className="field"><label>Mã đơn</label><input required value={maDon} onChange={(e) => setMaDon(e.target.value)} placeholder="VD: VL261112ABCDE" /></div>
          <div className="field"><label>Số điện thoại người đặt</label><input required value={sdt} onChange={(e) => setSdt(e.target.value)} /></div>
          <button className="btn">Tra cứu</button>
        </form>
      </div>
    </>
  )
}
