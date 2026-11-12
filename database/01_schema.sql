/* =====================================================================
   HỆ THỐNG BÁN TOUR DU LỊCH TỈNH VĨNH LONG (MỚI)
   Script tạo CSDL - MySQL 8.0+ / MariaDB 10.4+ (XAMPP)
   Ánh xạ 39 giải pháp thiết kế (Q01–Q39) + các bảng hệ thống/CMS
   Chạy trong phpMyAdmin (tab SQL) hoặc: mysql -u root < 01_schema.sql
   ===================================================================== */
CREATE DATABASE IF NOT EXISTS vinhlongtour
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vinhlongtour;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

/* =====================================================================
   PHÂN HỆ 0: HỆ THỐNG - TÀI KHOẢN (Q22, Q23) + Đăng nhập Google
   ===================================================================== */
CREATE TABLE VaiTro (
    VaiTroID     INT AUTO_INCREMENT PRIMARY KEY,
    MaVaiTro     VARCHAR(30)  NOT NULL UNIQUE,      -- ADMIN, KHACH_HANG
    TenVaiTro    VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE TaiKhoan (
    TaiKhoanID   INT AUTO_INCREMENT PRIMARY KEY,
    Email        VARCHAR(150) NOT NULL UNIQUE,
    MatKhauHash  VARCHAR(100) NULL,                 -- NULL nếu chỉ đăng nhập Google
    HoTen        VARCHAR(150) NOT NULL,
    AnhDaiDien   VARCHAR(500) NULL,
    NhaCungCap   VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',    -- LOCAL | GOOGLE
    GoogleSub    VARCHAR(100) NULL UNIQUE,          -- 'sub' trong Google ID token (UNIQUE cho phép nhiều NULL)
    VaiTroID     INT          NOT NULL,
    TrangThai    VARCHAR(20)  NOT NULL DEFAULT 'HOAT_DONG', -- HOAT_DONG | KHOA
    NgayTao      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_TaiKhoan_VaiTro FOREIGN KEY (VaiTroID) REFERENCES VaiTro(VaiTroID),
    CONSTRAINT CK_TaiKhoan_NhaCungCap CHECK (NhaCungCap IN ('LOCAL','GOOGLE'))
) ENGINE=InnoDB;

/* Q22-Q23: KhachHang tách khỏi TaiKhoan; TaiKhoanID NULL = khách vãng lai */
CREATE TABLE KhachHang (
    KhachHangID  INT AUTO_INCREMENT PRIMARY KEY,
    TaiKhoanID   INT          NULL UNIQUE,
    HoTen        VARCHAR(150) NOT NULL,
    SoDienThoai  VARCHAR(20)  NULL,
    Email        VARCHAR(150) NULL,
    DiaChi       VARCHAR(300) NULL,
    NgayTao      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_KhachHang_TaiKhoan FOREIGN KEY (TaiKhoanID) REFERENCES TaiKhoan(TaiKhoanID),
    INDEX IX_KhachHang_SDT (SoDienThoai),
    INDEX IX_KhachHang_Email (Email)
) ENGINE=InnoDB;

/* =====================================================================
   NHÓM 1: THÔNG TIN TOUR & ĐIỂM ĐẾN (Q01–Q07)
   ===================================================================== */
-- Q01
CREATE TABLE Tour (
    TourID       INT AUTO_INCREMENT PRIMARY KEY,
    MaTour       VARCHAR(30)  NOT NULL UNIQUE,
    TenTour      VARCHAR(250) NOT NULL,
    MoTa         LONGTEXT     NULL,
    ThoiLuong    VARCHAR(50)  NOT NULL,             -- VD: '2 ngày 1 đêm'
    SoNgay       INT          NOT NULL DEFAULT 1,
    TrangThai    VARCHAR(20)  NOT NULL DEFAULT 'NHAP', -- NHAP | DANG_BAN | NGUNG_BAN
    NoiBat       BIT          NOT NULL DEFAULT 0,
    NgayTao      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    NgayCapNhat  DATETIME     NULL
) ENGINE=InnoDB;

-- Q02 (N-N)
CREATE TABLE LoaiHinh (
    LoaiHinhID   INT AUTO_INCREMENT PRIMARY KEY,
    TenLoaiHinh  VARCHAR(150) NOT NULL UNIQUE,
    MoTa         VARCHAR(500) NULL
) ENGINE=InnoDB;
CREATE TABLE Tour_LoaiHinh (
    TourID       INT NOT NULL,
    LoaiHinhID   INT NOT NULL,
    PRIMARY KEY (TourID, LoaiHinhID),
    CONSTRAINT FK_TLH_Tour FOREIGN KEY (TourID) REFERENCES Tour(TourID) ON DELETE CASCADE,
    CONSTRAINT FK_TLH_LoaiHinh FOREIGN KEY (LoaiHinhID) REFERENCES LoaiHinh(LoaiHinhID)
) ENGINE=InnoDB;

-- Q03 (N-N)
CREATE TABLE KhuVuc (
    KhuVucID     INT AUTO_INCREMENT PRIMARY KEY,
    TenKhuVuc    VARCHAR(150) NOT NULL UNIQUE,
    MoTa         VARCHAR(500) NULL
) ENGINE=InnoDB;
CREATE TABLE Tour_KhuVuc (
    TourID       INT NOT NULL,
    KhuVucID     INT NOT NULL,
    PRIMARY KEY (TourID, KhuVucID),
    CONSTRAINT FK_TKV_Tour FOREIGN KEY (TourID) REFERENCES Tour(TourID) ON DELETE CASCADE,
    CONSTRAINT FK_TKV_KhuVuc FOREIGN KEY (KhuVucID) REFERENCES KhuVuc(KhuVucID)
) ENGINE=InnoDB;

-- Q04-Q05: Điểm đến độc lập, KHÔNG nối trực tiếp N-N với Tour
CREATE TABLE DiemDen (
    DiemDenID    INT AUTO_INCREMENT PRIMARY KEY,
    TenDiemDen   VARCHAR(200)  NOT NULL,
    KhuVucID     INT           NULL,
    DiaChi       VARCHAR(300)  NULL,
    ViDo         DECIMAL(9,6)  NULL,
    KinhDo       DECIMAL(9,6)  NULL,
    MoTa         LONGTEXT      NULL,
    HinhAnh      VARCHAR(500)  NULL,
    CONSTRAINT FK_DiemDen_KhuVuc FOREIGN KEY (KhuVucID) REFERENCES KhuVuc(KhuVucID)
) ENGINE=InnoDB;

-- Q06
CREATE TABLE HinhAnhTour (
    HinhAnhID    INT AUTO_INCREMENT PRIMARY KEY,
    TourID       INT          NOT NULL,
    DuongDan     VARCHAR(500) NOT NULL,
    LaAnhDaiDien BIT          NOT NULL DEFAULT 0,
    ThuTuHienThi INT          NOT NULL DEFAULT 0,
    CONSTRAINT FK_HinhAnh_Tour FOREIGN KEY (TourID) REFERENCES Tour(TourID) ON DELETE CASCADE,
    INDEX IX_HinhAnhTour_Tour (TourID, ThuTuHienThi)
) ENGINE=InnoDB;

-- Q07
CREATE TABLE DichVu (
    DichVuID     INT AUTO_INCREMENT PRIMARY KEY,
    TenDichVu    VARCHAR(200) NOT NULL UNIQUE,
    BieuTuong    VARCHAR(50)  NULL
) ENGINE=InnoDB;
CREATE TABLE Tour_DichVu (
    TourDichVuID INT AUTO_INCREMENT PRIMARY KEY,
    TourID       INT          NOT NULL,
    DichVuID     INT          NOT NULL,
    LoaiApDung   VARCHAR(20)  NOT NULL,             -- BAO_GOM | KHONG_BAO_GOM
    GhiChu       VARCHAR(300) NULL,
    CONSTRAINT UQ_Tour_DichVu UNIQUE (TourID, DichVuID),
    CONSTRAINT FK_TDV_Tour FOREIGN KEY (TourID) REFERENCES Tour(TourID) ON DELETE CASCADE,
    CONSTRAINT FK_TDV_DichVu FOREIGN KEY (DichVuID) REFERENCES DichVu(DichVuID),
    CONSTRAINT CK_Tour_DichVu_Loai CHECK (LoaiApDung IN ('BAO_GOM','KHONG_BAO_GOM'))
) ENGINE=InnoDB;

/* =====================================================================
   NHÓM 3 (phần 1): ĐỢT KHỞI HÀNH (Q14, Q15)
   ===================================================================== */
CREATE TABLE DotKhoiHanh (
    DotKhoiHanhID INT AUTO_INCREMENT PRIMARY KEY,
    TourID        INT         NOT NULL,
    MaDot         VARCHAR(40) NOT NULL UNIQUE,
    NgayGioDi     DATETIME    NOT NULL,
    NgayGioVe     DATETIME    NOT NULL,
    SucChuaToiDa  INT         NOT NULL,
    SoChoDaDat    INT         NOT NULL DEFAULT 0,
    HanNhanKhach  DATETIME    NOT NULL,
    TrangThai     VARCHAR(20) NOT NULL DEFAULT 'MO_BAN', -- MO_BAN | HET_CHO | DONG | DA_KHOI_HANH | HUY
    CONSTRAINT FK_Dot_Tour FOREIGN KEY (TourID) REFERENCES Tour(TourID),
    CONSTRAINT CK_Dot_ThoiGian CHECK (NgayGioVe >= NgayGioDi),
    CONSTRAINT CK_Dot_SucChua  CHECK (SoChoDaDat >= 0 AND SoChoDaDat <= SucChuaToiDa),
    INDEX IX_Dot_Tour (TourID, NgayGioDi)
) ENGINE=InnoDB;

/* =====================================================================
   NHÓM 2: CẤU TRÚC LỊCH TRÌNH (Q08–Q13)
   ===================================================================== */
-- Q08 + Q13: gắn vào Tour (lịch trình mẫu) HOẶC DotKhoiHanh (phiên bản theo đợt)
CREATE TABLE NgayLichTrinh (
    NgayLichTrinhID INT AUTO_INCREMENT PRIMARY KEY,
    TourID          INT NULL,
    DotKhoiHanhID   INT NULL,
    NgayThuMay      INT          NOT NULL,
    TieuDeNgay      VARCHAR(250) NOT NULL,
    CONSTRAINT FK_NgayLT_Tour FOREIGN KEY (TourID) REFERENCES Tour(TourID),
    CONSTRAINT FK_NgayLT_Dot  FOREIGN KEY (DotKhoiHanhID) REFERENCES DotKhoiHanh(DotKhoiHanhID),
    CONSTRAINT CK_NgayLichTrinh_ChuSoHuu CHECK (
        (TourID IS NOT NULL AND DotKhoiHanhID IS NULL) OR
        (TourID IS NULL AND DotKhoiHanhID IS NOT NULL)),
    INDEX IX_NgayLT_TourNgay (TourID, NgayThuMay),
    INDEX IX_NgayLT_DotNgay  (DotKhoiHanhID, NgayThuMay)
) ENGINE=InnoDB;

-- Q09, Q10, Q11, Q12
CREATE TABLE HoatDongLichTrinh (
    HoatDongID      INT AUTO_INCREMENT PRIMARY KEY,
    NgayLichTrinhID INT           NOT NULL,
    LoaiHoatDong    VARCHAR(20)   NOT NULL,       -- DI_CHUYEN | AN_UONG | THAM_QUAN | NGHI_NGOI | KHAC
    GioBatDau       TIME          NULL,
    GioKetThuc      TIME          NULL,
    NoiDung         VARCHAR(1000) NOT NULL,
    ThuTuHienThi    INT           NOT NULL DEFAULT 0,
    DiemDenID       INT           NULL,           -- Optional (Q11-Q12)
    CONSTRAINT FK_HoatDong_Ngay FOREIGN KEY (NgayLichTrinhID) REFERENCES NgayLichTrinh(NgayLichTrinhID) ON DELETE CASCADE,
    CONSTRAINT FK_HoatDong_DiemDen FOREIGN KEY (DiemDenID) REFERENCES DiemDen(DiemDenID),
    CONSTRAINT CK_HoatDong_Loai CHECK (LoaiHoatDong IN ('DI_CHUYEN','AN_UONG','THAM_QUAN','NGHI_NGOI','KHAC')),
    INDEX IX_HoatDong_NgayThuTu (NgayLichTrinhID, ThuTuHienThi)
) ENGINE=InnoDB;

/* =====================================================================
   NHÓM 3 (phần 2): BẢNG GIÁ & PHỤ THU (Q16–Q21)
   ===================================================================== */
-- Q16-Q19
CREATE TABLE LoaiKhach (
    LoaiKhachID   INT AUTO_INCREMENT PRIMARY KEY,
    TenLoaiKhach  VARCHAR(100) NOT NULL UNIQUE,
    TuoiTu        INT NOT NULL,
    TuoiDen       INT NOT NULL,
    HeSoChiemCho  INT NOT NULL DEFAULT 1,          -- Q19: 1 ghế riêng, 0 ngồi chung
    CONSTRAINT CK_LoaiKhach_Tuoi CHECK (TuoiTu <= TuoiDen),
    CONSTRAINT CK_LoaiKhach_HeSo CHECK (HeSoChiemCho IN (0,1))
) ENGINE=InnoDB;
CREATE TABLE BangGiaDot (
    BangGiaID     INT AUTO_INCREMENT PRIMARY KEY,
    DotKhoiHanhID INT NOT NULL,
    LoaiKhachID   INT NOT NULL,
    DonGia        DECIMAL(18,0) NOT NULL,
    CONSTRAINT UQ_BangGiaDot UNIQUE (DotKhoiHanhID, LoaiKhachID),
    CONSTRAINT FK_BangGia_Dot FOREIGN KEY (DotKhoiHanhID) REFERENCES DotKhoiHanh(DotKhoiHanhID) ON DELETE CASCADE,
    CONSTRAINT FK_BangGia_LoaiKhach FOREIGN KEY (LoaiKhachID) REFERENCES LoaiKhach(LoaiKhachID),
    CONSTRAINT CK_BangGia_DonGia CHECK (DonGia >= 0)
) ENGINE=InnoDB;

-- Q20
CREATE TABLE DiemDon (
    DiemDonID     INT AUTO_INCREMENT PRIMARY KEY,
    TenDiemDon    VARCHAR(200) NOT NULL,
    DiaChi        VARCHAR(300) NULL,
    KhuVucID      INT NULL,
    CONSTRAINT FK_DiemDon_KhuVuc FOREIGN KEY (KhuVucID) REFERENCES KhuVuc(KhuVucID)
) ENGINE=InnoDB;
CREATE TABLE DotKhoiHanh_DiemDon (
    DotDiemDonID  INT AUTO_INCREMENT PRIMARY KEY,
    DotKhoiHanhID INT NOT NULL,
    DiemDonID     INT NOT NULL,
    GioDon        DATETIME NOT NULL,
    PhiPhuThuDon  DECIMAL(18,0) NOT NULL DEFAULT 0,  -- tính theo mỗi hành khách đón tại điểm này
    CONSTRAINT UQ_Dot_DiemDon UNIQUE (DotKhoiHanhID, DiemDonID),
    CONSTRAINT FK_DDD_Dot FOREIGN KEY (DotKhoiHanhID) REFERENCES DotKhoiHanh(DotKhoiHanhID) ON DELETE CASCADE,
    CONSTRAINT FK_DDD_DiemDon FOREIGN KEY (DiemDonID) REFERENCES DiemDon(DiemDonID)
) ENGINE=InnoDB;

-- Q21
CREATE TABLE PhuThu (
    PhuThuID      INT AUTO_INCREMENT PRIMARY KEY,
    TourID        INT NULL,                          -- NULL = áp dụng chung mọi tour
    TenPhuThu     VARCHAR(200) NOT NULL,
    CachTinh      VARCHAR(20)  NOT NULL,             -- THEO_NGUOI | THEO_DON | THEO_SO_LUONG
    DonGia        DECIMAL(18,0) NOT NULL,
    ConHieuLuc    BIT NOT NULL DEFAULT 1,
    CONSTRAINT FK_PhuThu_Tour FOREIGN KEY (TourID) REFERENCES Tour(TourID),
    CONSTRAINT CK_PhuThu_CachTinh CHECK (CachTinh IN ('THEO_NGUOI','THEO_DON','THEO_SO_LUONG')),
    CONSTRAINT CK_PhuThu_DonGia CHECK (DonGia >= 0)
) ENGINE=InnoDB;

/* =====================================================================
   NHÓM 6 (phần 1): CHÍNH SÁCH HỦY (Q37)
   ===================================================================== */
CREATE TABLE ChinhSachHuy (
    ChinhSachHuyID INT AUTO_INCREMENT PRIMARY KEY,
    TourID         INT NULL,                         -- NULL = chính sách chung
    TenChinhSach   VARCHAR(200) NOT NULL,
    PhienBan       INT NOT NULL DEFAULT 1,
    TuNgay         DATE NOT NULL,
    DenNgay        DATE NULL,
    NoiDung        LONGTEXT NULL,
    CONSTRAINT FK_CSH_Tour FOREIGN KEY (TourID) REFERENCES Tour(TourID)
) ENGINE=InnoDB;
CREATE TABLE MocPhatHuy (
    MocPhatID       INT AUTO_INCREMENT PRIMARY KEY,
    ChinhSachHuyID  INT NOT NULL,
    SoNgayTruocTu   INT NOT NULL,    -- hủy trước ngày đi từ X ngày ...
    SoNgayTruocDen  INT NOT NULL,    -- ... đến Y ngày (bao gồm)
    PhanTramPhat    DECIMAL(5,2) NOT NULL,
    CONSTRAINT FK_MocPhat_CSH FOREIGN KEY (ChinhSachHuyID) REFERENCES ChinhSachHuy(ChinhSachHuyID) ON DELETE CASCADE,
    CONSTRAINT CK_MocPhat CHECK (SoNgayTruocTu <= SoNgayTruocDen),
    CONSTRAINT CK_MocPhat_PT CHECK (PhanTramPhat BETWEEN 0 AND 100)
) ENGINE=InnoDB;

/* =====================================================================
   NHÓM 4: ĐẶT TOUR & HÀNH KHÁCH (Q24–Q31)
   ===================================================================== */
CREATE TABLE DonDatTour (
    DonDatTourID   INT AUTO_INCREMENT PRIMARY KEY,
    MaDon          VARCHAR(30)   NOT NULL UNIQUE,
    DotKhoiHanhID  INT           NOT NULL,                      -- Q25
    KhachHangID    INT           NOT NULL,                      -- Q24
    TenNguoiDat    VARCHAR(150)  NOT NULL,                      -- Q29
    SDTNguoiDat    VARCHAR(20)   NOT NULL,
    EmailNguoiDat  VARCHAR(150)  NULL,
    TongTienVe     DECIMAL(18,0) NOT NULL DEFAULT 0,
    TongPhuThu     DECIMAL(18,0) NOT NULL DEFAULT 0,
    TongTien       DECIMAL(18,0) NOT NULL DEFAULT 0,
    DaThanhToan    DECIMAL(18,0) NOT NULL DEFAULT 0,
    SoChoChiem     INT           NOT NULL DEFAULT 0,
    GhiChu         VARCHAR(1000) NULL,
    NgayDat        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    HanGiuCho      DATETIME      NULL,                          -- Q31
    TrangThai      VARCHAR(20)   NOT NULL DEFAULT 'CHO_THANH_TOAN',
        -- CHO_THANH_TOAN | DA_COC | DA_THANH_TOAN | HOAN_THANH | DA_HUY | HET_HAN
    ChinhSachHuyID INT           NULL,                          -- Q37
    CONSTRAINT FK_Don_Dot FOREIGN KEY (DotKhoiHanhID) REFERENCES DotKhoiHanh(DotKhoiHanhID),
    CONSTRAINT FK_Don_KhachHang FOREIGN KEY (KhachHangID) REFERENCES KhachHang(KhachHangID),
    CONSTRAINT FK_Don_CSH FOREIGN KEY (ChinhSachHuyID) REFERENCES ChinhSachHuy(ChinhSachHuyID),
    INDEX IX_Don_GiuCho (TrangThai, HanGiuCho)
) ENGINE=InnoDB;

-- Q26, Q27, Q28
CREATE TABLE HanhKhach (
    HanhKhachID    INT AUTO_INCREMENT PRIMARY KEY,
    DonDatTourID   INT           NOT NULL,
    HoTen          VARCHAR(150)  NOT NULL,
    NgaySinh       DATE          NOT NULL,
    GioiTinh       VARCHAR(10)   NULL,       -- NAM | NU | KHAC
    SoDienThoai    VARCHAR(20)   NULL,
    CCCD_Passport  VARCHAR(30)   NULL,
    LoaiKhachID    INT           NOT NULL,
    DiemDonID      INT           NULL,
    GiaApDung      DECIMAL(18,0) NOT NULL,   -- Q28: chốt giá tại thời điểm đặt
    PhiDon         DECIMAL(18,0) NOT NULL DEFAULT 0,
    TrangThai      VARCHAR(20)   NOT NULL DEFAULT 'HIEU_LUC', -- HIEU_LUC | DA_HUY
    CONSTRAINT FK_HK_Don FOREIGN KEY (DonDatTourID) REFERENCES DonDatTour(DonDatTourID) ON DELETE CASCADE,
    CONSTRAINT FK_HK_LoaiKhach FOREIGN KEY (LoaiKhachID) REFERENCES LoaiKhach(LoaiKhachID),
    CONSTRAINT FK_HK_DiemDon FOREIGN KEY (DiemDonID) REFERENCES DiemDon(DiemDonID)
) ENGINE=InnoDB;

-- Q21 (chi tiết phụ thu của đơn)
CREATE TABLE ChiTietPhuThuDon (
    ChiTietPhuThuID INT AUTO_INCREMENT PRIMARY KEY,
    DonDatTourID    INT NOT NULL,
    PhuThuID        INT NOT NULL,
    SoLuong         INT NOT NULL DEFAULT 1,
    DonGia          DECIMAL(18,0) NOT NULL,
    ThanhTien       DECIMAL(18,0) NOT NULL,
    CONSTRAINT FK_CTPT_Don FOREIGN KEY (DonDatTourID) REFERENCES DonDatTour(DonDatTourID) ON DELETE CASCADE,
    CONSTRAINT FK_CTPT_PhuThu FOREIGN KEY (PhuThuID) REFERENCES PhuThu(PhuThuID)
) ENGINE=InnoDB;

-- Q30
CREATE TABLE LichSuTrangThaiDon (
    LichSuID        INT AUTO_INCREMENT PRIMARY KEY,
    DonDatTourID    INT NOT NULL,
    TrangThaiCu     VARCHAR(20) NULL,
    TrangThaiMoi    VARCHAR(20) NOT NULL,
    NguoiThayDoiID  INT NULL,                -- NULL = hệ thống tự động
    ThoiGian        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LyDo            VARCHAR(500) NULL,
    CONSTRAINT FK_LS_Don FOREIGN KEY (DonDatTourID) REFERENCES DonDatTour(DonDatTourID) ON DELETE CASCADE,
    CONSTRAINT FK_LS_TaiKhoan FOREIGN KEY (NguoiThayDoiID) REFERENCES TaiKhoan(TaiKhoanID),
    INDEX IX_LichSu_DonTG (DonDatTourID, ThoiGian)
) ENGINE=InnoDB;

/* =====================================================================
   NHÓM 5: THANH TOÁN & ĐỐI SOÁT (Q32–Q34)
   ===================================================================== */
CREATE TABLE ThanhToan (
    ThanhToanID     INT AUTO_INCREMENT PRIMARY KEY,
    DonDatTourID    INT NOT NULL,
    SoTien          DECIMAL(18,0) NOT NULL,
    LoaiThanhToan   VARCHAR(20) NOT NULL,     -- COC | TAT_TOAN | TOAN_BO
    PhuongThuc      VARCHAR(20) NOT NULL,     -- VNPAY | CHUYEN_KHOAN | TIEN_MAT
    MaThamChieu     VARCHAR(50) NULL UNIQUE,  -- vnp_TxnRef gửi sang VNPay
    MaGiaoDich      VARCHAR(50) NULL,         -- vnp_TransactionNo / mã GD ngân hàng
    MaNganHang      VARCHAR(20) NULL,
    ThoiGianTT      DATETIME NULL,
    AnhChungTu      VARCHAR(500) NULL,
    NguoiXacNhanID  INT NULL,
    TrangThai       VARCHAR(20) NOT NULL DEFAULT 'CHO_THANH_TOAN', -- CHO_THANH_TOAN | THANH_CONG | THAT_BAI
    DuLieuPhanHoi   LONGTEXT NULL,            -- log thô phản hồi cổng thanh toán
    NgayTao         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_TT_Don FOREIGN KEY (DonDatTourID) REFERENCES DonDatTour(DonDatTourID),
    CONSTRAINT FK_TT_TaiKhoan FOREIGN KEY (NguoiXacNhanID) REFERENCES TaiKhoan(TaiKhoanID),
    CONSTRAINT CK_TT_SoTien CHECK (SoTien > 0)
) ENGINE=InnoDB;

/* =====================================================================
   NHÓM 6 (phần 2): HỦY, HOÀN TIỀN, CHUYỂN ĐỢT (Q35, Q36, Q38, Q39)
   ===================================================================== */
CREATE TABLE YeuCauHuy (
    YeuCauHuyID      INT AUTO_INCREMENT PRIMARY KEY,
    DonDatTourID     INT NOT NULL,
    LoaiHuy          VARCHAR(20) NOT NULL,    -- TOAN_BO | TUNG_PHAN
    LyDo             VARCHAR(1000) NULL,
    ThoiGianYeuCau   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    NguoiYeuCauID    INT NULL,
    NguoiXuLyID      INT NULL,
    ThoiGianXuLy     DATETIME NULL,
    TongPhiPhat      DECIMAL(18,0) NOT NULL DEFAULT 0,
    SoTienDuKienHoan DECIMAL(18,0) NOT NULL DEFAULT 0,
    KetQua           VARCHAR(20) NOT NULL DEFAULT 'CHO_XU_LY', -- CHO_XU_LY | CHAP_NHAN | TU_CHOI
    GhiChuXuLy       VARCHAR(1000) NULL,
    CONSTRAINT FK_YCH_Don FOREIGN KEY (DonDatTourID) REFERENCES DonDatTour(DonDatTourID),
    CONSTRAINT FK_YCH_NguoiYC FOREIGN KEY (NguoiYeuCauID) REFERENCES TaiKhoan(TaiKhoanID),
    CONSTRAINT FK_YCH_NguoiXL FOREIGN KEY (NguoiXuLyID) REFERENCES TaiKhoan(TaiKhoanID)
) ENGINE=InnoDB;
CREATE TABLE ChiTietHuyHanhKhach (
    ChiTietHuyID    INT AUTO_INCREMENT PRIMARY KEY,
    YeuCauHuyID     INT NOT NULL,
    HanhKhachID     INT NOT NULL,
    PhiPhat         DECIMAL(18,0) NOT NULL DEFAULT 0,
    CONSTRAINT UQ_ChiTietHuy UNIQUE (YeuCauHuyID, HanhKhachID),
    CONSTRAINT FK_CTH_YCH FOREIGN KEY (YeuCauHuyID) REFERENCES YeuCauHuy(YeuCauHuyID) ON DELETE CASCADE,
    CONSTRAINT FK_CTH_HK FOREIGN KEY (HanhKhachID) REFERENCES HanhKhach(HanhKhachID)
) ENGINE=InnoDB;

CREATE TABLE HoanTien (
    HoanTienID      INT AUTO_INCREMENT PRIMARY KEY,
    DonDatTourID    INT NOT NULL,
    YeuCauHuyID     INT NULL,
    SoTienHoan      DECIMAL(18,0) NOT NULL,
    NgayHoan        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PhuongThuc      VARCHAR(20) NOT NULL DEFAULT 'CHUYEN_KHOAN',
    MaGiaoDichHoan  VARCHAR(50) NULL,
    AnhChungTu      VARCHAR(500) NULL,
    NguoiThucHienID INT NULL,
    GhiChu          VARCHAR(500) NULL,
    CONSTRAINT FK_HT_Don FOREIGN KEY (DonDatTourID) REFERENCES DonDatTour(DonDatTourID),
    CONSTRAINT FK_HT_YCH FOREIGN KEY (YeuCauHuyID) REFERENCES YeuCauHuy(YeuCauHuyID),
    CONSTRAINT FK_HT_TaiKhoan FOREIGN KEY (NguoiThucHienID) REFERENCES TaiKhoan(TaiKhoanID),
    CONSTRAINT CK_HT_SoTien CHECK (SoTienHoan > 0)
) ENGINE=InnoDB;

CREATE TABLE LichSuChuyenDot (
    ChuyenDotID     INT AUTO_INCREMENT PRIMARY KEY,
    DonDatTourID    INT NOT NULL,
    DotCuID         INT NOT NULL,
    DotMoiID        INT NOT NULL,
    LyDo            VARCHAR(500) NULL,
    ChenhLechTien   DECIMAL(18,0) NOT NULL DEFAULT 0,  -- dương = khách trả thêm, âm = hoàn lại
    NguoiThucHienID INT NULL,
    ThoiGian        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_LSCD_Don FOREIGN KEY (DonDatTourID) REFERENCES DonDatTour(DonDatTourID),
    CONSTRAINT FK_LSCD_DotCu FOREIGN KEY (DotCuID) REFERENCES DotKhoiHanh(DotKhoiHanhID),
    CONSTRAINT FK_LSCD_DotMoi FOREIGN KEY (DotMoiID) REFERENCES DotKhoiHanh(DotKhoiHanhID),
    CONSTRAINT FK_LSCD_TaiKhoan FOREIGN KEY (NguoiThucHienID) REFERENCES TaiKhoan(TaiKhoanID)
) ENGINE=InnoDB;

/* =====================================================================
   PHÂN HỆ CMS: GIỚI THIỆU, TIN TỨC, LIÊN HỆ
   ===================================================================== */
CREATE TABLE CauHinhTrang (
    KhoaCauHinh  VARCHAR(50) PRIMARY KEY,   -- GIOI_THIEU, HOTLINE, EMAIL, DIA_CHI, ...
    GiaTri       LONGTEXT    NULL,
    NgayCapNhat  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE TinTuc (
    TinTucID     INT AUTO_INCREMENT PRIMARY KEY,
    TieuDe       VARCHAR(300) NOT NULL,
    Slug         VARCHAR(320) NOT NULL UNIQUE,
    TomTat       VARCHAR(600) NULL,
    NoiDung      LONGTEXT     NOT NULL,
    AnhBia       VARCHAR(500) NULL,
    TrangThai    VARCHAR(20)  NOT NULL DEFAULT 'NHAP',  -- NHAP | XUAT_BAN
    LuotXem      INT          NOT NULL DEFAULT 0,
    TacGiaID     INT          NULL,
    NgayDang     DATETIME     NULL,
    NgayTao      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_TinTuc_TaiKhoan FOREIGN KEY (TacGiaID) REFERENCES TaiKhoan(TaiKhoanID)
) ENGINE=InnoDB;

CREATE TABLE LienHe (
    LienHeID     INT AUTO_INCREMENT PRIMARY KEY,
    HoTen        VARCHAR(150)  NOT NULL,
    Email        VARCHAR(150)  NOT NULL,
    SoDienThoai  VARCHAR(20)   NULL,
    TieuDe       VARCHAR(250)  NULL,
    NoiDung      VARCHAR(2000) NOT NULL,
    TrangThai    VARCHAR(20)   NOT NULL DEFAULT 'MOI',   -- MOI | DA_XU_LY
    NgayGui      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
