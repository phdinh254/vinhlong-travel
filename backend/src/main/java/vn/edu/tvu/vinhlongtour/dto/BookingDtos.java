package vn.edu.tvu.vinhlongtour.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class BookingDtos {
    private BookingDtos() {}

    public record PassengerInput(
            @NotBlank @Size(max = 150) String hoTen,
            @NotNull @PastOrPresent LocalDate ngaySinh,
            @Pattern(regexp = "NAM|NU|KHAC") String gioiTinh,
            @Size(max = 20) String soDienThoai,
            @Size(max = 30) String cccdPassport,
            Integer diemDonId) {}

    public record SurchargeInput(@NotNull Integer phuThuId, @Min(1) Integer soLuong) {}

    public record CreateBookingRequest(
            @NotNull Integer dotKhoiHanhId,
            @NotBlank @Size(max = 150) String tenNguoiDat,
            @NotBlank @Pattern(regexp = "^[0-9+]{9,15}$", message = "số điện thoại không hợp lệ") String sdtNguoiDat,
            @Email @Size(max = 150) String emailNguoiDat,
            @Size(max = 1000) String ghiChu,
            @NotEmpty @Valid List<PassengerInput> hanhKhachs,
            @Valid List<SurchargeInput> phuThus) {}

    public record PassengerQuote(String hoTen, Integer tuoi, Integer loaiKhachId, String tenLoaiKhach,
                                 BigDecimal giaApDung, BigDecimal phiDon, Integer heSoChiemCho) {}

    public record SurchargeLine(Integer phuThuId, String tenPhuThu, String cachTinh, Integer soLuong,
                                BigDecimal donGia, BigDecimal thanhTien) {}

    public record Quote(List<PassengerQuote> hanhKhachs, List<SurchargeLine> phuThus, BigDecimal tongTienVe,
                        BigDecimal tongPhiDon, BigDecimal tongPhuThu, BigDecimal tongTien, Integer soChoChiem,
                        BigDecimal soTienCocToiThieu) {}

    public record PassengerDto(Integer hanhKhachId, String hoTen, LocalDate ngaySinh, String gioiTinh, String soDienThoai,
                               String cccdPassport, String tenLoaiKhach, BigDecimal giaApDung, BigDecimal phiDon,
                               Integer diemDonId, String tenDiemDon, String trangThai) {}

    public record PaymentDto(Integer thanhToanId, BigDecimal soTien, String loaiThanhToan, String phuongThuc,
                             String maGiaoDich, String maNganHang, LocalDateTime thoiGianTT, String trangThai,
                             String anhChungTu, String nguoiXacNhan, LocalDateTime ngayTao) {}

    public record HistoryDto(String trangThaiCu, String trangThaiMoi, LocalDateTime thoiGian, String lyDo, String nguoiThayDoi) {}

    public record CancelDto(Integer yeuCauHuyId, String loaiHuy, String lyDo, LocalDateTime thoiGianYeuCau, String ketQua,
                            BigDecimal tongPhiPhat, BigDecimal soTienDuKienHoan, String ghiChuXuLy,
                            LocalDateTime thoiGianXuLy, List<String> hanhKhachs, String maDon) {}

    public record RefundDto(Integer hoanTienId, Integer yeuCauHuyId, BigDecimal soTienHoan, LocalDateTime ngayHoan,
                            String phuongThuc, String maGiaoDichHoan, String anhChungTu, String ghiChu) {}

    public record TransferDto(String dotCu, String dotMoi, BigDecimal chenhLechTien, String lyDo, LocalDateTime thoiGian) {}

    public record BookingDetail(Integer donDatTourId, String maDon, String trangThai,
                                Integer tourId, String maTour, String tenTour,
                                Integer dotKhoiHanhId, String maDot, LocalDateTime ngayGioDi, LocalDateTime ngayGioVe,
                                String tenNguoiDat, String sdtNguoiDat, String emailNguoiDat, String ghiChu,
                                BigDecimal tongTienVe, BigDecimal tongPhuThu, BigDecimal tongTien, BigDecimal daThanhToan,
                                BigDecimal conLai, BigDecimal soTienCocToiThieu, Integer soChoChiem,
                                LocalDateTime ngayDat, LocalDateTime hanGiuCho, boolean laKhachVangLai,
                                List<PassengerDto> hanhKhachs, List<SurchargeLine> phuThus, List<PaymentDto> thanhToans,
                                List<HistoryDto> lichSu, List<CancelDto> yeuCauHuys, List<RefundDto> hoanTiens,
                                List<TransferDto> chuyenDots, TourDtos.PolicyDto chinhSachHuy) {}

    public record BookingSummary(Integer donDatTourId, String maDon, String tenTour, String maDot, LocalDateTime ngayGioDi,
                                 String tenNguoiDat, String sdtNguoiDat, Integer soHanhKhach, BigDecimal tongTien,
                                 BigDecimal daThanhToan, String trangThai, LocalDateTime ngayDat) {}

    /** sdt dùng để xác minh với khách vãng lai */
    public record CancelRequestInput(@NotBlank @Pattern(regexp = "TOAN_BO|TUNG_PHAN") String loaiHuy,
                                     @Size(max = 1000) String lyDo, List<Integer> hanhKhachIds, String sdt) {}

    public record CreatePaymentRequest(@NotBlank String maDon, String sdt,
                                       @NotBlank @Pattern(regexp = "COC|CON_LAI") String loai) {}

    public record PaymentUrlResponse(String paymentUrl, BigDecimal soTien) {}

    public record VnPayResult(boolean hopLe, boolean thanhCong, String maDon, BigDecimal soTien,
                              String maGiaoDich, String maPhanHoi, String thongBao) {}
}
