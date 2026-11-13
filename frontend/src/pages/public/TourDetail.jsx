import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import api, { errMsg, imgUrl, money, fmtDateTime, TRANG_THAI_DOT, CACH_TINH_PHU_THU } from '../../api'
import { Badge, Loading, Alert } from '../../components/ui.jsx'
import Itinerary from '../../components/Itinerary.jsx'

export default function TourDetail() {
  const { id } = useParams()
  const [t, setT] = useState(null)
  const [err, setErr] = useState('')
  const [dotXem, setDotXem] = useState(null)
  const [lichTrinhDot, setLichTrinhDot] = useState(null)

  useEffect(() => {
    api.get(`/api/public/tours/${id}`).then((r) => setT(r.data)).catch((e) => setErr(errMsg(e)))
  }, [id])

  // Nút "Đặt tour" ở thẻ tour dẫn tới #lich-khoi-hanh: cuộn tới khi dữ liệu đã tải
  useEffect(() => {
    if (t && window.location.hash === '#lich-khoi-hanh') document.getElementById('lich-khoi-hanh')?.scrollIntoView({ behavior: 'smooth' })
  }, [t])

  const xemLichTrinhDot = async (d) => {
    setDotXem(d)
    setLichTrinhDot((await api.get(`/api/public/dots/${d.dotKhoiHanhId}/lich-trinh`)).data)
  }

  if (err) return <div className="container section"><Alert>{err}</Alert></div>
  if (!t) return <div className="container section"><Loading /></div>

  const imgs = t.hinhAnhs.map((h) => imgUrl(h.duongDan))
  const main = imgUrl(t.hinhAnhs.find((h) => h.laAnhDaiDien)?.duongDan) || imgs[0]
  const baoGom = t.dichVus.filter((d) => d.loaiApDung === 'BAO_GOM')
  const khong = t.dichVus.filter((d) => d.loaiApDung === 'KHONG_BAO_GOM')

  return (
    <div className="container section">
      <div className="chips" style={{ marginBottom: 8 }}>
        {t.khuVucs.map((k) => <span key={k.id} className="badge info">{k.ten}</span>)}
        {t.loaiHinhs.map((k) => <span key={k.id} className="badge">{k.ten}</span>)}
      </div>
      <h1>{t.tenTour}</h1>
      <p className="muted">Mã tour: {t.maTour} · Thời lượng: {t.thoiLuong}</p>

      <div className="gallery">
        <div className="main" style={main ? { backgroundImage: `url(${main})` } : undefined} />
        <div className="side">
          {imgs.filter((x) => x !== main).slice(0, 3).map((x) => <div key={x} style={{ backgroundImage: `url(${x})` }} />)}
        </div>
      </div>

      <div className="grid grid-main" style={{ marginTop: 24 }}>
        <div className="stack">
          <div className="card"><h2>Giới thiệu tour</h2><div className="prose" dangerouslySetInnerHTML={{ __html: t.moTa || '' }} /></div>
          <div className="card"><h2>Lịch trình</h2><Itinerary ngays={t.lichTrinh} /></div>
          <div className="card grid grid-2">
            <div><h3>✅ Bao gồm</h3><ul>{baoGom.map((d) => <li key={d.dichVuId}>{d.tenDichVu}{d.ghiChu && <span className="muted"> – {d.ghiChu}</span>}</li>)}</ul></div>
            <div><h3>❌ Không bao gồm</h3><ul>{khong.map((d) => <li key={d.dichVuId}>{d.tenDichVu}{d.ghiChu && <span className="muted"> – {d.ghiChu}</span>}</li>)}</ul></div>
          </div>
          {t.chinhSachHuy && (
            <div className="card">
              <h2>Chính sách hủy</h2>
              <p className="muted small">{t.chinhSachHuy.tenChinhSach} (phiên bản {t.chinhSachHuy.phienBan})</p>
              <table><thead><tr><th>Hủy trước ngày khởi hành</th><th>Phí phạt</th></tr></thead><tbody>
                {t.chinhSachHuy.mocPhats.map((m, i) => (
                  <tr key={i}><td>{m.soNgayTruocDen >= 9999 ? `Từ ${m.soNgayTruocTu} ngày trở lên` : `${m.soNgayTruocTu} – ${m.soNgayTruocDen} ngày`}</td><td>{m.phanTramPhat}%</td></tr>
                ))}
              </tbody></table>
            </div>
          )}
        </div>

        <aside className="stack">
          <div className="card">
            <h3 id="lich-khoi-hanh" style={{ scrollMarginTop: 90 }}>Lịch khởi hành</h3>
            {t.dots.length === 0 && <p className="muted">Hiện chưa có đợt khởi hành mở bán.</p>}
            <p className="small" style={{ margin: '0 0 12px' }}>
              Cần tư vấn hoặc muốn đi theo nhóm riêng? <Link to={`/lien-he?tourId=${t.tourId}`}>Hỏi về tour này →</Link>
            </p>
            {t.dots.map((d) => (
              <div key={d.dotKhoiHanhId} className="box" style={{ marginBottom: 10 }}>
                <div className="row between">
                  <strong>{fmtDateTime(d.ngayGioDi)}</strong>
                  <Badge map={TRANG_THAI_DOT} value={d.trangThai} />
                </div>
                <div className="small muted">Về: {fmtDateTime(d.ngayGioVe)} · Còn {d.soChoConLai}/{d.sucChuaToiDa} chỗ</div>
                <div className="small" style={{ margin: '6px 0' }}>
                  {d.bangGia.map((g) => <div key={g.loaiKhachId} className="row between"><span>{g.tenLoaiKhach} ({g.tuoiTu}–{g.tuoiDen >= 150 ? '…' : g.tuoiDen} tuổi)</span><strong>{money(g.donGia)}</strong></div>)}
                </div>
                <div className="row">
                  {d.trangThai === 'MO_BAN'
                    ? <Link className="btn sm" to={`/dat-tour/${d.dotKhoiHanhId}`}>Đặt tour</Link>
                    : <button className="btn sm" disabled>Hết chỗ</button>}
                  {d.coLichTrinhRieng && <button className="btn link small" onClick={() => xemLichTrinhDot(d)}>Lịch trình riêng của đợt</button>}
                </div>
              </div>
            ))}
          </div>
          {t.phuThus.length > 0 && (
            <div className="card">
              <h3>Phụ thu tùy chọn</h3>
              {t.phuThus.map((p) => <div key={p.phuThuId} className="summary-line small"><span>{p.tenPhuThu} <span className="muted">({CACH_TINH_PHU_THU[p.cachTinh]})</span></span><span>{money(p.donGia)}</span></div>)}
            </div>
          )}
        </aside>
      </div>

      {dotXem && (
        <div className="modal-bg" onMouseDown={(e) => e.target === e.currentTarget && setDotXem(null)}>
          <div className="modal">
            <div className="row between"><h3>Lịch trình đợt {dotXem.maDot}</h3><button className="btn link" onClick={() => setDotXem(null)}>✕</button></div>
            {lichTrinhDot ? <Itinerary ngays={lichTrinhDot} /> : <Loading />}
          </div>
        </div>
      )}
    </div>
  )
}
