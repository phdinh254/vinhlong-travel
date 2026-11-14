-- Giai đoạn 1: tài khoản đăng nhập (khách hàng và quản trị viên)
create table accounts (
    id            bigserial primary key,
    email         varchar(150) not null unique,   -- luôn lưu chữ thường
    password_hash varchar(100),                   -- null: tài khoản chỉ đăng nhập bằng Google
    full_name     varchar(150) not null,
    phone         varchar(20),
    avatar_url    varchar(500),
    google_sub    varchar(64) unique,             -- mã người dùng Google, null nếu chưa liên kết
    role          varchar(20) not null check (role in ('ADMIN', 'CUSTOMER')),
    status        varchar(20) not null check (status in ('ACTIVE', 'LOCKED')),
    created_at    timestamptz not null
);
