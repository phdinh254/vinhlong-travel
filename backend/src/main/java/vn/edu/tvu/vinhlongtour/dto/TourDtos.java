package vn.edu.tvu.vinhlongtour.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class TourDtos {
    private TourDtos() {}

    public record Option(Integer id, String ten) {}

    public record TourCard(Integer tourId, String maTour, String tenTour, String thoiLuong, String anhDaiDien,
                           BigDecimal giaTu, List<String> loaiHinhs, List<String> khuVucs, Boolean noiBat,
                           String trangThai, String moTaNgan) {}

    public record ImageDto(Integer hinhAnhId, String duongDan, Boolean laAnhDaiDien, Integer thuTuHienThi) {}

    public record DichVuDto(Integer dichVuId, String tenDichVu, String bieuTuong, String loaiApDung, String ghiChu) {}

    public record HoatDongDto(Integer hoatDongId, String loaiHoatDong, LocalTime gioBatDau, LocalTime gioKetThuc,
                              String noiDung, Integer thuTuHienThi, Integer diemDenId, String tenDiemDen) {}

    public record NgayDto(Integer ngayLichTrinhId, Integer ngayThuMay, String tieuDeNgay, List<HoatDongDto> hoatDongs) {}

    public record GiaDto(Integer loaiKhachId, String tenLoaiKhach, Integer tuoiTu, Integer tuoiDen,
                         Integer heSoChiemCho, BigDecimal donGia) {}

    public record DiemDonDto(Integer diemDonId, String tenDiemDon, String diaChi, LocalDateTime gioDon, BigDecimal phiPhuThuDon) {}

    public record DotDto(Integer dotKhoiHanhId, Integer tourId, String maDot, LocalDateTime ngayGioDi, LocalDateTime ngayGioVe,
                         Integer sucChuaToiDa, Integer soChoDaDat, Integer soChoConLai, LocalDateTime hanNhanKhach,
                         String trangThai, List<GiaDto> bangGia, List<DiemDonDto> diemDons, boolean coLichTrinhRieng) {}

    public record PhuThuDto(Integer phuThuId, Integer tourId, String tenPhuThu, String cachTinh, BigDecimal donGia, Boolean conHieuLuc) {}

    public record MocDto(@NotNull @Min(0) Integer soNgayTruocTu, @NotNull @Min(0) Integer soNgayTruocDen,
                         @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal phanTramPhat) {}

    public record PolicyDto(Integer chinhSachHuyId, Integer tourId, String tenChinhSach, Integer phienBan,
                            LocalDate tuNgay, LocalDate denNgay, String noiDung, List<MocDto> mocPhats) {}

    public record TourDetail(Integer tourId, String maTour, String tenTour, String moTa, String thoiLuong, Integer soNgay,
                             String trangThai, Boolean noiBat, List<Option> loaiHinhs, List<Option> khuVucs,
                             List<ImageDto> hinhAnhs, List<DichVuDto> dichVus, List<NgayDto> lichTrinh,
                             List<DotDto> dots, List<PhuThuDto> phuThus, PolicyDto chinhSachHuy) {}

    /* ---------- Request (admin) ---------- */
    public record ImageInput(@NotBlank String duongDan, Boolean laAnhDaiDien, Integer thuTuHienThi) {}

    public record DichVuInput(@NotNull Integer dichVuId, @NotBlank @Pattern(regexp = "BAO_GOM|KHONG_BAO_GOM") String loaiApDung, String ghiChu) {}

    public record HoatDongInput(@NotBlank @Pattern(regexp = "DI_CHUYEN|AN_UONG|THAM_QUAN|NGHI_NGOI|KHAC") String loaiHoatDong,
                                LocalTime gioBatDau, LocalTime gioKetThuc, @NotBlank String noiDung,
                                Integer thuTuHienThi, Integer diemDenId) {}

    public record NgayInput(@NotNull @Min(1) Integer ngayThuMay, @NotBlank String tieuDeNgay,
                            @Valid List<HoatDongInput> hoatDongs) {}

    public record TourSaveRequest(
            @NotBlank @Size(max = 30) String maTour,
            @NotBlank @Size(max = 250) String tenTour,
            String moTa,
            @NotBlank @Size(max = 50) String thoiLuong,
            @NotNull @Min(1) Integer soNgay,
            @NotBlank @Pattern(regexp = "NHAP|DANG_BAN|NGUNG_BAN") String trangThai,
            Boolean noiBat,
            List<Integer> loaiHinhIds,
            List<Integer> khuVucIds,
            @Valid List<ImageInput> hinhAnhs,
            @Valid List<DichVuInput> dichVus,
            @Valid List<NgayInput> lichTrinh) {}

    public record GiaInput(@NotNull Integer loaiKhachId, @NotNull @DecimalMin("0") BigDecimal donGia) {}

    public record DiemDonInput(@NotNull Integer diemDonId, @NotNull LocalDateTime gioDon, @DecimalMin("0") BigDecimal phiPhuThuDon) {}

    public record DotSaveRequest(
            @NotNull Integer tourId,
            @NotBlank @Size(max = 40) String maDot,
            @NotNull LocalDateTime ngayGioDi,
            @NotNull LocalDateTime ngayGioVe,
            @NotNull @Min(1) Integer sucChuaToiDa,
            @NotNull LocalDateTime hanNhanKhach,
            @Pattern(regexp = "MO_BAN|HET_CHO|DONG|DA_KHOI_HANH|HUY") String trangThai,
            @Valid List<GiaInput> bangGia,
            @Valid List<DiemDonInput> diemDons,
            Boolean saoChepLichTrinh) {}
}
