package vn.edu.tvu.vinhlongtour.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {}

    public record ManualPaymentRequest(@NotNull @DecimalMin("1") BigDecimal soTien,
                                       @NotBlank @Pattern(regexp = "COC|TAT_TOAN|TOAN_BO") String loaiThanhToan,
                                       @NotBlank @Pattern(regexp = "CHUYEN_KHOAN|TIEN_MAT") String phuongThuc,
                                       String maGiaoDich, String anhChungTu) {}

    public record UpdateStatusRequest(@NotBlank @Pattern(regexp = "HOAN_THANH|DA_HUY") String trangThai, String lyDo) {}

    public record ProcessCancelRequest(@NotNull Boolean chapNhan, String ghiChu) {}

    public record RefundRequest(Integer yeuCauHuyId, @NotNull @DecimalMin("1") BigDecimal soTienHoan,
                                @Pattern(regexp = "CHUYEN_KHOAN|TIEN_MAT|VNPAY") String phuongThuc,
                                String maGiaoDichHoan, String anhChungTu, String ghiChu) {}

    public record TransferRequest(@NotNull Integer dotMoiId, String lyDo) {}

    public record Dashboard(long tongTour, long tongDon, long donChoThanhToan, long donDaCoc, long donDaThanhToan,
                            BigDecimal tongDaThu, long lienHeMoi, long yeuCauHuyCho,
                            List<BookingDtos.BookingSummary> donGanDay) {}

    public record NewsSaveRequest(@NotBlank @Size(max = 300) String tieuDe, @Size(max = 320) String slug,
                                  @Size(max = 600) String tomTat, @NotBlank String noiDung, String anhBia,
                                  @NotBlank @Pattern(regexp = "NHAP|XUAT_BAN") String trangThai) {}

    public record NewsDto(Integer tinTucId, String tieuDe, String slug, String tomTat, String noiDung, String anhBia,
                          String trangThai, Integer luotXem, LocalDateTime ngayDang, LocalDateTime ngayTao) {}

    public record DiemDenInput(@NotBlank String tenDiemDen, Integer khuVucId, String diaChi, BigDecimal viDo,
                               BigDecimal kinhDo, String moTa, String hinhAnh) {}

    public record DiemDonInput(@NotBlank String tenDiemDon, String diaChi, Integer khuVucId) {}

    public record PhuThuInput(Integer tourId, @NotBlank String tenPhuThu,
                              @NotBlank @Pattern(regexp = "THEO_NGUOI|THEO_DON|THEO_SO_LUONG") String cachTinh,
                              @NotNull @DecimalMin("0") BigDecimal donGia, Boolean conHieuLuc) {}

    public record PolicyInput(Integer tourId, @NotBlank String tenChinhSach, @NotNull @Min(1) Integer phienBan,
                              @NotNull LocalDate tuNgay, LocalDate denNgay, String noiDung,
                              @NotEmpty @Valid List<TourDtos.MocDto> mocPhats) {}

    public record UserDto(Integer taiKhoanId, String email, String hoTen, String vaiTro, String nhaCungCap,
                          String trangThai, LocalDateTime ngayTao) {}

    public record ContactInput(@NotBlank @Size(max = 150) String hoTen, @NotBlank @Email String email,
                               @Size(max = 20) String soDienThoai, @Size(max = 250) String tieuDe,
                               @NotBlank @Size(max = 2000) String noiDung) {}
}
